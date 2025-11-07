package com.munch1182.lib.helper

import android.Manifest
import android.annotation.SuppressLint
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import androidx.annotation.RequiresPermission
import com.munch1182.lib.AppHelper
import com.munch1182.lib.base.OnUpdateListener
import com.munch1182.lib.base.Releasable
import com.munch1182.lib.base.log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.min

/**
 * 录音辅助
 *
 * @see record 开始录音并返回音频数据，录制动作跟随协程生命周期
 * @see release 销毁资源
 */
class RecordHelper(
    val sampleRate: Int = 44100, // 采样率，44100Hz兼容
    val channelConfig: Int = AudioFormat.CHANNEL_IN_MONO, // 单声道双声道
    val audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT, // 数据位宽，16BIT兼容
) : Releasable {

    companion object {

        /**
         * 音频识别
         */
        fun recognition() = RecordHelper(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
    }

    private val log = log()
    private val record by lazy { newRecord() }
    val bufferSize by lazy { AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat) }

    @SuppressLint("MissingPermission")
    private fun newRecord() = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, bufferSize)

    /**
     * 开始录音并返回音频数据
     *
     * 当数据接收时，录制开始；
     * 当协程取消时，录制也同步取消
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun record(time: Long = 40L) = callbackFlow {
        log.logStr("start record")
        record.startRecording()
        val maxSize = bufferSize
        val buff = ByteArray(maxSize)
        try {
            while (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                delay(time) // 协程取消时发出CancellationException跳出循环
                val size = record.read(buff, 0, maxSize)
                when {
                    size > 0 -> send(buff.copyOfRange(0, min(size, maxSize)))

                    size == AudioRecord.ERROR_INVALID_OPERATION -> {
                        close(IllegalStateException("ERROR_INVALID_OPERATION"))
                        break
                    }

                    size == AudioRecord.ERROR_BAD_VALUE -> {
                        close(IllegalStateException("ERROR_BAD_VALUE"))
                        break
                    }
                }
            }
        } catch (e: Exception) {
            close(e)
        } finally {
            log.logStr("finally, stop record")
            if (record.state == AudioRecord.STATE_INITIALIZED) record.stop()
        }
        awaitClose {}
    }

    override fun release() {
        record.release()
    }
}

/**
 * 边写边播的播放器
 *
 * 初始化时可能会抛出异常；
 * 参数必须与源音频一致, 否则会播放不正常
 *
 * @see prepare 准备写入和播放
 * @see write 写入数据并同步播放
 * @see writeOver 数据写入完成，等待播放完成回调
 * @see release 销毁资源
 */
class AudioStreamHelper @Throws(IllegalArgumentException::class) constructor(val sampleRate: Int, val channelMask: Int, val encoding: Int, val scale: Int = 1) : Releasable {

    private val log = log()
    val bufferSize by lazy { AudioTrack.getMinBufferSize(sampleRate, channelMask, encoding) * scale }
    var format: AudioFormat
        private set
    var player: AudioTrack
        private set

    init {
        try {
            format = AudioFormat.Builder().setEncoding(encoding).setSampleRate(sampleRate).setChannelMask(channelMask).build()
            player = AudioTrack.Builder().setAudioFormat(format).setBufferSizeInBytes(bufferSize).setTransferMode(AudioTrack.MODE_STREAM) // 边写边播
                .build()
        } catch (e: Exception) {
            log.logStr("init error with sampleRate:${sampleRate} channelMask:${channelMask} encoding:$encoding bufferSize:$bufferSize")
            log.log(e)
            e.printStackTrace()
            throw e
        }
    }

    val state get() = player.playState
    val newBuff get() = ByteArray(bufferSize)
    private val fsib by lazy { format.frameSizeInBytesCompat }

    private val callback = object : AudioTrack.OnPlaybackPositionUpdateListener {
        override fun onMarkerReached(track: AudioTrack?) {
            log.logStr("onMarkerReached: markerPos: ${track?.notificationMarkerPosition}, written: $written")
            over?.invoke()
            over = null
        }

        override fun onPeriodicNotification(track: AudioTrack?) {
        }
    }
    private var written = 0
    private var over: (() -> Unit)? = null

    /**
     * 准备播放和数据写入
     */
    fun prepare() {
        log.logStr("prepare")
        if (state == AudioTrack.PLAYSTATE_PLAYING) return
        written = 0
        player.play()
        player.setPlaybackPositionUpdateListener(callback)
    }

    /**
     * 需要保证线程安全，否则多线程同时写入会导致播放错误，也可能会导致原生方法的缓存溢出而抛出崩溃
     * 因为要循环多次调用该方法，所以线程安全交由外部实现
     *
     * @see prepare
     */
    fun write(data: ByteArray, offset: Int = 0, len: Int = data.size) {
        player.write(data, offset, len)
        written += len / fsib
    }

    /**
     * 设置播放结束的回调，当前面写入的数据播放完成后，会执行回调
     */
    fun writeOver(over: (() -> Unit)? = null) {
        log.logStr("write over")
        this.over = over
        player.setNotificationMarkerPosition(written)
    }

    /**
     * 停止播放
     */
    fun stop() {
        log.logStr("stop")
        player.stop()
    }

    override fun release() {
        log.logStr("release")
        stop()
        player.release()
    }
}

val AudioFormat.frameSizeInBytesCompat
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        frameSizeInBytes
    } else {
        // see android.media.AudioFormat#getBytesPerSample
        when (encoding) {
            AudioFormat.ENCODING_PCM_8BIT, AudioFormat.ENCODING_MP3 -> 1
            AudioFormat.ENCODING_PCM_16BIT, AudioFormat.ENCODING_IEC61937, AudioFormat.ENCODING_DEFAULT -> 2
            AudioFormat.ENCODING_PCM_24BIT_PACKED -> 3
            AudioFormat.ENCODING_PCM_FLOAT, AudioFormat.ENCODING_PCM_32BIT -> 4
            else -> 2
        } * channelCount
    }

/**
 * 通过[RecordHelper]的参数创建[AudioStreamHelper]
 */
@Throws(IllegalArgumentException::class)
fun RecordHelper.toAudio(scale: Int = 1) = AudioStreamHelper(
    sampleRate, if (channelConfig == AudioFormat.CHANNEL_IN_MONO) AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO, audioFormat, scale
)

/**
 * 音频焦点工具类
 */
class AudioFocusHelper : ARManager<OnUpdateListener<Boolean>> by ARDefaultManager(), Releasable {
    private val log = log()
    private val am by lazy { AudioHelper.am }

    /**
     * 只会回调成功和失败，并且成功不一定会回调
     *
     * 一般回调场景：请求焦点后其它应用再请求焦点(且不能同时播放)，当前焦点就会被打断并回调
     */
    private val callback = AudioManager.OnAudioFocusChangeListener {
        val loss = AudioFocus.from(it * -1)
        log.logStr("onAudioFocusChange: ${loss?.let { "LOSS_$loss" } ?: it.toString()}")
        forEach { l -> l.onUpdate(it == AudioManager.AUDIOFOCUS_GAIN) }
    }
    private var currReq: AudioFocusRequest? = null

    /**
     * 请求音频焦点
     *
     * @return 返回是否请求成功；[AudioManager.OnAudioFocusChangeListener]并不一定被触发
     */
    fun requestAudioFocusNow(focus: AudioFocus, handler: Handler? = null): Boolean {
        abandonAudioFocus()
        val request = AudioFocusRequest.Builder(focus.value).apply {
            if (handler != null) setOnAudioFocusChangeListener(callback, handler) else setOnAudioFocusChangeListener(callback)
        }.build()
        currReq = request

        val result = am.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        log.logStr("requestAudioFocusNow: ${focus}: $result")
        return result
    }

    fun abandonAudioFocus(): Boolean {
        // 要调用此方法来让系统解除OnAudioFocusChangeListener的注册
        val result = currReq?.let {
            val result = am.abandonAudioFocusRequest(it) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            log.logStr("abandonAudioFocus: ${currReq?.focusGain?.let { i -> AudioFocus.from(i) }}: $result")
            result
        }
        currReq = null
        return result ?: true
    }

    override fun release() {
        clear()
        abandonAudioFocus()
    }
}

sealed class AudioFocus(val value: Int) {
    /**
     *  打断其它播放
     */
    object Gain : AudioFocus(AudioManager.AUDIOFOCUS_GAIN)

    /**
     * 打断其它播放，清除后恢复其它播放
     */
    object GainTransient : AudioFocus(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)

    /**
     * 打断其它播放，清除后恢复其它播放；请求期间系统不应该播放任何通知，用于语音录制或者识别
     */
    object GainTransientExclusive : AudioFocus(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)

    /**
     * 其它播放声音变小作为背景音，清除后声音恢复
     */
    object GainTransientMayDuck : AudioFocus(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)

    companion object {
        fun from(value: Int) = when (value) {
            AudioManager.AUDIOFOCUS_GAIN -> Gain
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT -> GainTransient
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE -> GainTransientExclusive
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK -> GainTransientMayDuck
            else -> null
        }
    }

    override fun toString() = when (this) {
        Gain -> "Gain"
        GainTransient -> "GainTransient"
        GainTransientExclusive -> "GainTransientExclusive"
        GainTransientMayDuck -> "GainTransientMayDuck"
    }
}

object AudioHelper {
    val am: AudioManager get() = AppHelper.getSystemService(AudioManager::class.java)
    val inputs: Array<out AudioDeviceInfo> get() = am.getDevices(AudioManager.GET_DEVICES_INPUTS)
    val outputs: Array<out AudioDeviceInfo> get() = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
}