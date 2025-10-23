package com.munch1182.p1.views

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.munch1182.lib.AppHelper
import com.munch1182.lib.base.launchMain
import com.munch1182.lib.base.log
import com.munch1182.lib.helper.NetStateHelper
import com.munch1182.lib.helper.result.isAllGranted
import com.munch1182.lib.helper.result.permissions
import com.munch1182.p1.App
import com.munch1182.p1.base.BaseActivity
import com.munch1182.p1.base.bind
import com.munch1182.p1.databinding.ActivityNetPhoneBinding
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.CandidatePairChangeEvent
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoTrack
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class NetPhoneActivity : BaseActivity() {

    companion object {
        private const val WS_SERVER_URL = "ws://192.168.2.130:8888/ws"
    }

    private val log = log()
    private val netState by lazy { runCatching { NetStateHelper() }.getOrNull() }
    private val eglBase by lazy { EglBase.create() }
    private val bind by bind(ActivityNetPhoneBinding::inflate)
    private val localVideoView by lazy { bind.localVideoView.init(eglBase?.eglBaseContext, null) }
    private val remoteVideoView by lazy { bind.remoteVideoView.init(eglBase?.eglBaseContext, null) }
    private lateinit var peerConnectionFactory: PeerConnectionFactory
    private var videoCapturer: CameraVideoCapturer? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private var localVideoTrack: VideoTrack? = null
    private var localAudioTrack: AudioTrack? = null
    private var socket: WebSocket? = null
    private var localPeerConnection: PeerConnection? = null
    private var remotePeerConnection: PeerConnection? = null

    override fun onDestroy() {
        super.onDestroy()
        destroy()
    }

    private fun destroy() {
        runCatching { videoCapturer?.dispose() }
        runCatching { surfaceTextureHelper?.dispose() }
        runCatching { localVideoTrack?.dispose() }
        runCatching { localAudioTrack?.dispose() }
        runCatching { localPeerConnection?.dispose() }
        runCatching { remotePeerConnection?.dispose() }
        runCatching { socket?.close(1000, null) }
        runCatching { peerConnectionFactory.dispose() }
        runCatching { eglBase.release() }
    }

    @OptIn(ExperimentalUuidApi::class)
    private val my = Uuid.random().toString().replace("-", "")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initViews()
        withPermission(false) {
            initWebRTC()
            initNetState()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initNetState() {
        val netState = netState ?: return
        netState.register(App.instance.appHandler)
        netState.add {
            lifecycleScope.launchMain { bind.netStatus.text = if (!it.isConnected) "当前网络：无" else "当前网络：${it.type}" }
        }

        lifecycleScope.launchMain {
            lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    super.onDestroy(owner)
                    netState.release()
                }
            })
        }
    }

    private fun withPermission(reInit: Boolean = true, p: () -> Unit) {
        permissions(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.ACCESS_NETWORK_STATE)).request {
            if (it.isAllGranted()) {
                if (reInit && videoCapturer == null) initWebRTC()
                p()
            }
        }
    }

    private fun initViews() {
        fitWindow(bind.main)
        localVideoView
        remoteVideoView
        bind.remoteVideoView.setMirror(true)
        bind.btnStartCall.setOnClickListener { withPermission { startCall() } }
        bind.btnEndCall.setOnClickListener { withPermission { endCall() } }
        bind.btnSwitchCamera.setOnClickListener { withPermission { switchCamera() } }
    }

    private fun startCall() {
        enableStartCall(false)
        connectWebSocket()
        startLocalVideo()
    }


    private fun connectWebSocket(roomId: String = "1234") {
        val uri = "$WS_SERVER_URL?room=$roomId"
        socket = OkHttpClient().newWebSocket(Request.Builder().url(uri).build(), object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                super.onOpen(webSocket, response)
                updateState("已连接到信令服务器")
                log.logStr("已连接到服务器")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                super.onMessage(webSocket, text)
                handleSignalingMessage(text)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                super.onClosed(webSocket, code, reason)
                updateState("已断开连接")
                log.logStr("已断开连接")
                enableStartCall(true)
                socket = null
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                super.onFailure(webSocket, t, response)
                updateState("连接失败")
                log.logStr("连接失败: ${t.message}")
                enableStartCall(true)
                socket = null
            }
        })
    }

    private fun handleSignalingMessage(text: String) {
        log.logStr("收到信令消息: $text")
        val json = runCatching { JSONObject(text) }.getOrNull()
        if (json == null) {
            log.logStr("信令消息格式错误: $text")
            return
        }
        val type = json.getString("type")
        log.logStr("收到${type}消息")
        when (type) {
            "user-joined" -> createOffer()
            "offer" -> handleOffer(json)
            "answer" -> handleAnswer(json)
            "ice-candidate" -> handleIceCandidate(json)
        }
    }

    private fun handleIceCandidate(json: JSONObject) {
        val candidate = runCatching {
            IceCandidate(
                json.getString("sdpMid"), json.getInt("sdpMLineIndex"), json.getString("candidate")
            )
        }.getOrNull()
        if (candidate == null) {
            log.logStr("ice-candidate消息格式错误: $json")
            return
        }
        log.logStr("ice-candidate: $candidate")
        parseIceCandidate(candidate)
        localPeerConnection?.addIceCandidate(candidate)
        remotePeerConnection?.addIceCandidate(candidate)
    }

    private fun parseIceCandidate(candidate: IceCandidate?) {
        candidate ?: return
        val type = IceType.fromString(candidate.sdp)
        log.logStr("ice类型: $type")
    }

    private fun handleAnswer(json: JSONObject) {
        val sdp = runCatching { json.getString("sdp") }.getOrNull()
        if (sdp == null) {
            log.logStr("answer消息格式错误: $json")
            return
        }
        log.logStr("answer: $sdp")
        val answer = SessionDescription(SessionDescription.Type.ANSWER, sdp)
        localPeerConnection?.setRemoteDescription(SdpServer("远程描述"), answer)
    }

    private fun handleOffer(json: JSONObject) {
        val sdp = runCatching { json.getString("sdp") }.getOrNull()
        if (sdp == null) {
            log.logStr("offer消息格式错误: $json")
            return
        }
        val offer = SessionDescription(SessionDescription.Type.OFFER, sdp)
        remotePeerConnection = createPeerConnection()
        localVideoTrack?.let {
            val sender = remotePeerConnection?.addTrack(it, listOf("remote_video"))
            log.logStr("添加远程视频轨道: ${sender?.track()?.id()}")
        }
        localAudioTrack?.let {
            val sender = remotePeerConnection?.addTrack(it, listOf("remote_audio"))
            log.logStr("添加远程音频轨道: ${sender?.track()?.id()}")
        }
        remotePeerConnection?.setRemoteDescription(object : SdpServer("远程描述") {
            override fun onSetSuccess() {
                super.onSetSuccess()
                createAnswer()
            }
        }, offer)
    }

    private fun createAnswer() {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }
        remotePeerConnection?.createAnswer(object : SdpServer("answer") {
            override fun onCreateSuccess(p0: SessionDescription?) {
                super.onCreateSuccess(p0)
                if (p0 == null) {
                    log.logStr("answer消息为空")
                    return
                }
                remotePeerConnection?.setLocalDescription(object : SdpServer("本地描述") {
                    override fun onSetSuccess() {
                        super.onSetSuccess()
                        sendSessionDescription(p0)
                    }
                }, p0)
            }
        }, constraints)

    }

    private fun createOffer() {
        localPeerConnection = createPeerConnection()
        log.logStr("创建offer: localPeerConnection: $localPeerConnection")
        localVideoTrack?.let {
            val sender = localPeerConnection?.addTrack(it, listOf("local_video"))
            log.logStr("添加本地视频轨道: ${sender?.track()?.id()}")
        }
        localAudioTrack?.let {
            val sender = localPeerConnection?.addTrack(it, listOf("local_audio"))
            log.logStr("添加本地音频轨道: ${sender?.track()?.id()}")
        }
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }
        localPeerConnection?.createOffer(object : SdpServer("offer") {
            override fun onCreateSuccess(p0: SessionDescription?) {
                super.onCreateSuccess(p0)
                localPeerConnection?.setLocalDescription(object : SdpServer("本地描述") {
                    override fun onSetSuccess() {
                        super.onSetSuccess()
                        p0?.let { sendSessionDescription(it) }
                    }
                }, p0)
            }
        }, constraints)
    }

    private fun sendSessionDescription(desc: SessionDescription) {
        val msg = JSONObject().apply {
            put("type", desc.type.canonicalForm())
            put("sdp", desc.description)
        }
        log.logStr("发送信令消息: $msg")
        sendMsg(msg.toString())
    }

    private fun sendIceCandidate(candidate: IceCandidate) {
        val message = JSONObject().apply {
            put("type", "ice-candidate")
            put("sdpMid", candidate.sdpMid)
            put("sdpMLineIndex", candidate.sdpMLineIndex)
            put("candidate", candidate.sdp)
            put("serverUrl", candidate.serverUrl)
            put("adapterType", candidate.adapterType.name)
        }
        log.logStr("发送ICE候选者: $message")
        parseIceCandidate(candidate)
        sendMsg(message.toString())
    }

    private fun createPeerConnection(): PeerConnection? {
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
            //PeerConnection.IceServer.builder("192.168.2.130:3478").createIceServer(),
        )
        return peerConnectionFactory.createPeerConnection(iceServers, object : PeerConnection.Observer {
            override fun onSignalingChange(p0: PeerConnection.SignalingState?) {
                log.logStr("信令状态改变: ${p0?.name}")
            }

            override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
                log.logStr("ICE连接状态改变: ${p0?.name}")
                when (p0) {
                    PeerConnection.IceConnectionState.NEW -> {}
                    PeerConnection.IceConnectionState.CHECKING -> updateState("正在检查网络")
                    PeerConnection.IceConnectionState.CONNECTED -> updateState("已连接")
                    PeerConnection.IceConnectionState.COMPLETED -> updateState("连接已结束")
                    PeerConnection.IceConnectionState.FAILED -> updateState("连接失败")
                    PeerConnection.IceConnectionState.DISCONNECTED -> updateState("连接已断开")
                    PeerConnection.IceConnectionState.CLOSED -> updateState("连接已关闭")
                    null -> {}
                }
            }

            override fun onIceConnectionReceivingChange(p0: Boolean) {
                log.logStr("ICE连接接收状态改变: $p0")
            }

            override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
                log.logStr("ICE收集状态改变: ${p0?.name}")
            }

            override fun onSelectedCandidatePairChanged(event: CandidatePairChangeEvent?) {
                super.onSelectedCandidatePairChanged(event)
                event ?: return
                val local = IceType.from(event.local)
                val remote = IceType.from(event.remote)
                log.logStr("ICE候选者对改变: local($local) == remove($remote): ${event.reason} ")
            }

            override fun onIceCandidate(p0: IceCandidate?) {
                log.logStr("收到ICE候选者: ${p0?.sdp}")
                if (p0 != null) sendIceCandidate(p0)
            }

            override fun onIceCandidatesRemoved(p0: Array<out IceCandidate?>?) {
                log.logStr("收到ICE候选者移除: ${p0?.joinToString(",") { it?.sdp.toString() }}")
            }

            override fun onAddStream(p0: MediaStream?) {
                log.logStr("收到远程媒体流: ${p0?.id}")
                runOnUiThread { p0?.videoTracks?.firstOrNull()?.addSink(bind.remoteVideoView) }
            }

            override fun onRemoveStream(p0: MediaStream?) {
                log.logStr("移除远程媒体流: ${p0?.id}")
            }

            override fun onDataChannel(p0: DataChannel?) {
                log.logStr("收到数据通道: ${p0?.id()}")
            }

            override fun onRenegotiationNeeded() {
                log.logStr("需要重新协商")
            }

            override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream?>?) {
                log.logStr("收到轨道: ${p0?.track()?.id()}")
            }
        })
    }

    private fun enableStartCall(enable: Boolean) {
        bind.btnStartCall.post {
            bind.btnStartCall.isEnabled = enable
            bind.btnEndCall.isEnabled = !enable
        }
    }

    private fun sendMsg(any: String) {
        try {
            socket?.send(any)
        } catch (e: Exception) {
            e.printStackTrace()
            log.logStr("发送消息失败")
        }
    }

    private fun endCall() {
        socket?.close(1000, null)
        stopLockTask()
        stopLocalVideo()
        updateState("通话结束")
        enableStartCall(true)
    }

    private fun startLocalVideo() {
        videoCapturer?.startCapture(1280, 720, 30)
        updateState("本地视频已启动")
    }

    private fun stopLocalVideo() {
        log.logStr("stopLocalVideo")
        videoCapturer?.stopCapture()
        localVideoTrack?.removeSink(bind.localVideoView)
        surfaceTextureHelper?.dispose()
    }

    private fun switchCamera() {
        videoCapturer?.switchCamera(null)
    }

    private fun initWebRTC(ctx: Context = AppHelper) {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(ctx).setEnableInternalTracer(true).setFieldTrials("WebRTC-H264HighProfile/Enabled/").createInitializationOptions()
        )

        val videoEncoderFactory = DefaultVideoEncoderFactory(
            eglBase?.eglBaseContext, true,  // 启用硬件编码
            true   // 启用硬件解码
        )
        val videoDecoderFactory = DefaultVideoDecoderFactory(eglBase?.eglBaseContext)

        val options = PeerConnectionFactory.Options()
        peerConnectionFactory = PeerConnectionFactory.builder().setOptions(options).setVideoEncoderFactory(videoEncoderFactory).setVideoDecoderFactory(videoDecoderFactory).createPeerConnectionFactory()
        createVideoAudioTracks()
    }

    private fun createVideoAudioTracks() {
        val videoSource = peerConnectionFactory.createVideoSource(false)
        videoCapturer = createCameraCapturer()
        surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase?.eglBaseContext)
        videoCapturer?.initialize(surfaceTextureHelper, this, videoSource.capturerObserver)

        localVideoTrack = peerConnectionFactory.createVideoTrack("${my}_VideoTrack", videoSource)
        localVideoTrack?.addSink(bind.localVideoView)

        val audioSource = peerConnectionFactory.createAudioSource(MediaConstraints())
        localAudioTrack = peerConnectionFactory.createAudioTrack("${my}_AudioTrack", audioSource)
    }

    private fun createCameraCapturer(): CameraVideoCapturer? {
        val enumerator = Camera2Enumerator(this)
        val deviceNames = enumerator.deviceNames

        val front = deviceNames.firstOrNull { enumerator.isFrontFacing(it) }
        if (front != null) return enumerator.createCapturer(front, null)
        val back = deviceNames.firstOrNull { enumerator.isFrontFacing(it) }
        if (back != null) return enumerator.createCapturer(back, null)
        return null
    }

    private fun updateState(state: String) {
        runOnUiThread { bind.tvStatus.text = state }
    }

    open inner class SdpServer(val name: String) : SdpObserver {
        override fun onCreateSuccess(p0: SessionDescription?) {
            log.logStr("创建${name}成功")
        }

        override fun onSetSuccess() {
            log.logStr("设置${name}成功")
        }

        override fun onCreateFailure(p0: String?) {
            log.logStr("创建${name}失败: $p0")
        }

        override fun onSetFailure(p0: String?) {
            log.logStr("设置${name}失败: $p0")
        }
    }

    sealed class IceType {
        object Host : IceType()
        object Srflx : IceType()
        object Prflx : IceType()
        object Relay : IceType()
        class Other(val type: String) : IceType()

        override fun toString() = when (this) {
            Host -> "host"
            Srflx -> "srflx"
            Prflx -> "prflx"
            Relay -> "relay"
            is Other -> type
        }

        companion object {
            fun fromString(s: String): IceType? {
                val name = "typ"
                if (!s.contains(name)) return null
                val index = s.indexOf(name) + 4
                val end = s.indexOf(" ", index)
                val str = runCatching { s.substring(index, end) }.getOrNull() ?: return null
                return when (str) {
                    "host" -> Host
                    "srflx" -> Srflx
                    "prflx" -> Prflx
                    "relay" -> Relay
                    else -> Other(str)
                }
            }

            fun from(s: IceCandidate) = fromString(s.sdp)
        }
    }

}

