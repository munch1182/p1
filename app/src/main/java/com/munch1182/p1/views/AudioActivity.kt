package com.munch1182.p1.views

import android.Manifest
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.util.fastFirstOrNull
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.munch1182.lib.AppHelper
import com.munch1182.lib.base.ThreadHelper
import com.munch1182.lib.base.asLive
import com.munch1182.lib.base.asStateFlow
import com.munch1182.lib.base.launchIO
import com.munch1182.lib.base.log
import com.munch1182.lib.helper.result.onGranted
import com.munch1182.lib.helper.result.permission
import com.munch1182.lib.helper.sound.AudioHelper
import com.munch1182.lib.helper.sound.RecordHelper
import com.munch1182.lib.helper.sound.calculateDB
import com.munch1182.p1.R
import com.munch1182.p1.base.BaseActivity
import com.munch1182.p1.base.DialogHelper
import com.munch1182.p1.base.permissionIntentDialogWithName
import com.munch1182.p1.base.toast
import com.munch1182.p1.ui.ClickButton
import com.munch1182.p1.ui.ComposeListView
import com.munch1182.p1.ui.Split
import com.munch1182.p1.ui.StateButton
import com.munch1182.p1.ui.setContentWithRv
import com.munch1182.p1.ui.theme.FontManySize
import com.munch1182.p1.ui.theme.PagePadding
import com.munch1182.p1.ui.theme.PagePaddingHalf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.properties.Delegates

class AudioActivity : BaseActivity() {

    private val log = log()
    private val audioVM by viewModels<AudioVM>()
    private val recordVM by viewModels<RecordVM>()
    private val pool by lazy { SoundPool.Builder().build() }
    private var soundId by Delegates.notNull<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentWithRv { Views() }
        pool.setOnLoadCompleteListener { soundPool, sampleId, status ->
            log.logStr("loadComplete $soundPool $sampleId $status")
        }
        soundId = pool.load(this, R.raw.key_tone, 1)
        recordVM.onPlay { playSound() }.onStop { playSound() }
    }

    @Composable
    private fun Views() {
        Ops()
        Split()
        Db()
    }

    @Composable
    private fun Db() {
        val db by recordVM.db.collectAsState()
        Text("DB：$db")
    }

    @Composable
    private fun Ops() {
        val focus by audioVM.isFocus.observeAsState(false)
        val listenMB by audioVM.listenMB.observeAsState(false)
        val isRecording by recordVM.isRecording.observeAsState(false)
        var inputType by remember { mutableStateOf("") }
        var outputType by remember { mutableStateOf("") }
        StateButton(if (focus) "清除音频焦点" else "获取音频焦点", focus) { audioVM.toggleFocus() }
        StateButton(if (listenMB) "停止监听按键" else "监听媒体按键", listenMB) { audioVM.toggleListen() }

        Split()

        Row(verticalAlignment = Alignment.CenterVertically) {
            ClickButton("输入设备") {
                showSelectDevicesView(audioVM.collectInputDevs(), recordVM.input) {
                    recordVM.setInputDevs(it)
                    inputType = it?.type?.toAudiTypeStr() ?: ""
                }
            }
            Text(inputType, modifier = Modifier.padding(horizontal = PagePaddingHalf))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            ClickButton("输出设备") {
                showSelectDevicesView(audioVM.collectOutputDevs(), recordVM.output) {
                    recordVM.setOutputDevs(it)
                    outputType = it?.type?.toAudiTypeStr() ?: ""
                }
            }
            Text(outputType, modifier = Modifier.padding(horizontal = PagePaddingHalf))
        }

        Split()
        ClickButton("播放音效") { playSound() }
        StateButton(if (isRecording) "结束录音" else "开始录音", isRecording) {
            withRecordPermission {
                if (!isRecording) {
                    recordVM.toggleRecord()
                } else {
                    recordVM.toggleRecord()
                }
            }
        }

    }

    private fun playSound() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            log.logStr("playSound: $soundId, ${AudioHelper.am.communicationDevice?.type}")
        } else {
            log.logStr("playSound: $soundId")
        }
        pool.play(soundId, 1f, 1f, 1, 0, 1f)
    }

    private fun showSelectDevicesView(devs: Array<out AudioDeviceInfo>?, routed: AudioDeviceInfo?, chose: (AudioDeviceInfo?) -> Unit) {
        devs ?: return
        DialogHelper.newBottom { ctx, d ->
            ComposeListView(ctx, devs.size, Modifier.padding(vertical = PagePadding)) {
                val dev = devs[it]
                if (it == 0) HorizontalDivider()
                Column(
                    modifier = Modifier.clickable {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            d?.cancel()
                            chose.invoke(dev)
                        } else {
                            toast("手机版本低于${Build.VERSION_CODES.P}, 无法设置")
                        }
                    }) {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = PagePadding, vertical = PagePaddingHalf)
                            .fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(dev.type.toAudiTypeStr(), fontWeight = FontWeight.ExtraBold)
                        Text(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) "(${dev.address}, ${dev.productName})" else "(${dev.productName})", fontSize = FontManySize)
                        Text(if (dev.sampleRates.isEmpty() || dev.sampleRates.size == 9) "*" else dev.sampleRates.joinToString("/"), fontSize = FontManySize)
                        Text(if (dev.channelMasks.isEmpty()) "*" else dev.channelMasks.joinToString { i -> i.toChannelStr() }, fontSize = FontManySize)
                        Text(if (dev.encodings.isEmpty()) "*" else dev.encodings.joinToString { i -> i.toEncodingStr() }, fontSize = FontManySize)
                        if (routed?.type != null && dev.type == routed.type) Text("ROUTED", fontWeight = FontWeight.ExtraBold)
                    }
                    HorizontalDivider()
                }
            }
        }.show()
    }

    private fun Int.toChannelStr(): String {
        return when (this) {
            AudioFormat.CHANNEL_IN_MONO -> "CHANNEL_IN_MONO"
            AudioFormat.CHANNEL_IN_STEREO -> "CHANNEL_IN_STEREO"
            AudioFormat.CHANNEL_IN_BACK -> "CHANNEL_IN_BACK"
            AudioFormat.CHANNEL_IN_LEFT -> "CHANNEL_IN_FRONT_BACK"
            AudioFormat.CHANNEL_IN_RIGHT -> "CHANNEL_IN_SIDE"
            else -> this.toString()
        }
    }

    private fun Int.toEncodingStr(): String {
        return when (this) {
            AudioFormat.ENCODING_PCM_8BIT -> "ENCODING_PCM_8BIT"
            AudioFormat.ENCODING_PCM_16BIT -> "ENCODING_PCM_16BIT"
            AudioFormat.ENCODING_PCM_FLOAT -> "ENCODING_PCM_FLOAT"
            AudioFormat.ENCODING_AC3 -> "ENCODING_AC3"
            AudioFormat.ENCODING_E_AC3 -> "ENCODING_E_AC3"
            AudioFormat.ENCODING_DTS -> "ENCODING_DTS"
            AudioFormat.ENCODING_DTS_HD -> "ENCODING_DTS_HD"
            AudioFormat.ENCODING_IEC61937 -> "ENCODING_IEC61937"
            AudioFormat.ENCODING_PCM_32BIT -> "ENCODING_PCM_32BIT"
            else -> this.toString()
        }
    }


    private fun Int.toAudiTypeStr(): String {
        val name = when (this) {
            AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "BUILTIN_EARPIECE"
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "BUILTIN_SPEAKER"
            AudioDeviceInfo.TYPE_WIRED_HEADSET -> "WIRED_HEADSET"
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "WIRED_HEADPHONES"
            AudioDeviceInfo.TYPE_LINE_ANALOG -> "WIRED_HEADPHONES"
            AudioDeviceInfo.TYPE_LINE_DIGITAL -> "WIRED_HEADPHONES"
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "BLUETOOTH_SCO"
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "BLUETOOTH_A2DP"
            AudioDeviceInfo.TYPE_BUILTIN_MIC -> "BUILTIN_MIC"
            AudioDeviceInfo.TYPE_FM_TUNER -> "FM_TUNER"
            AudioDeviceInfo.TYPE_TELEPHONY -> "TELEPHONY"
            AudioDeviceInfo.TYPE_USB_HEADSET -> "USB_HEADSET"
            AudioDeviceInfo.TYPE_REMOTE_SUBMIX -> "REMOTE_SUBMIX"
            AudioDeviceInfo.TYPE_BLE_HEADSET -> "BLE_HEADSET"
            AudioDeviceInfo.TYPE_BLE_SPEAKER -> "TYPE_BLE_SPEAKER"
            28 -> "TYPE_ECHO_REFERENCE"
            else -> this.toString()
        }
        return "$name($this)"
    }

    private fun withRecordPermission(any: () -> Unit) {
        permission(Manifest.permission.RECORD_AUDIO).permissionIntentDialogWithName("录音").onGranted(any)
    }
}

class AudioVM : ViewModel() {
    private val log = log()
    private var _isFocus = MutableLiveData(false)
    private var _listenMB = MutableLiveData(false)
    private val focusHelper by lazy { AudioHelper.FocusHelper() }
    private val mbHelper by lazy { AudioHelper.MediaButtonHelper() }
    private var player: MediaPlayer? = null

    val isFocus = _isFocus.asLive()
    val listenMB = _listenMB.asLive()

    init {
        mbHelper.add { log.logStr(it.toStr()) }
    }

    override fun onCleared() {
        super.onCleared()
        player?.release()
        player = null
        focusHelper.clear()
        mbHelper.release()
    }

    fun toggleFocus() {
        if (_isFocus.value == true) {
            val result = focusHelper.clearAudioFocus()
            _isFocus.postValue(!result)
        } else {
            val result = focusHelper.requestAudioFocus()
            _isFocus.postValue(result)
        }
    }

    fun toggleListen() {
        if (_listenMB.value == true) {
            mbHelper.unListen()
            player?.release()
            player = null
            _listenMB.postValue(false)
        } else {
            player = (player ?: fakePlay())
            player?.start()
            player?.pause()
            mbHelper.listen()
            _listenMB.postValue(true)
        }
    }

    fun collectInputDevs(): Array<out AudioDeviceInfo>? {
        return AudioHelper.am.getDevices(AudioManager.GET_DEVICES_INPUTS)
    }

    fun collectOutputDevs(): Array<out AudioDeviceInfo>? {
        return AudioHelper.am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
    }

    private fun fakePlay() = MediaPlayer.create(AppHelper, R.raw.lapple).apply { isLooping = true }

    private fun KeyEvent.toStr(): String {
        val action = when (action) {
            KeyEvent.ACTION_DOWN -> "按下"
            KeyEvent.ACTION_UP -> "抬起"
            else -> action.toString()
        }
        val name = when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY -> "播放"
            KeyEvent.KEYCODE_MEDIA_PAUSE -> "暂停"
            KeyEvent.KEYCODE_MEDIA_NEXT -> "下一首"
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> "上一首"
            KeyEvent.KEYCODE_HEADSETHOOK -> "挂断"
            else -> keyCode.toString()
        }
        return "按键：$name: $action"
    }

}

class RecordVM : ViewModel() {

    private val log = log()
    private var _isRecording = MutableLiveData(false)
    private val _db = MutableStateFlow(0.0)
    private var _input: AudioDeviceInfo? = null
    private var _output: AudioDeviceInfo? = null
    private var _toIsRecording = false

    val isRecording = _isRecording.asLive()
    val db = _db.asStateFlow()
    val input get() = _input
    val output get() = _output


    private var onStart: (() -> Unit)? = null
    private var onStop: (() -> Unit)? = null


    // RecordHelper必须在有权限之后创建，所以对其的所有访问都应在有权限之后
    private val recordHelper by lazy { RecordHelper() }
    private var recordJob: Job? = null
    private val communicationListener by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AudioManager.OnCommunicationDeviceChangedListener {
                log.logStr("onCommunicationDeviceChanged: ${it?.type}")
                if (it?.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {
                    startRecordImpl()
                } else {
                    stopRecordImpl()
                }
            }
        } else {
            null
        }
    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AudioHelper.am.addOnCommunicationDeviceChangedListener(ThreadHelper.newSingleExecutors, communicationListener!!)
        }
    }

    fun onPlay(any: () -> Unit): RecordVM {
        this.onStart = {
            log.logStr("onPlay")
            any()
        }
        return this
    }

    fun onStop(any: () -> Unit): RecordVM {
        this.onStop = {
            log.logStr("onStop")
            any()
        }
        return this
    }

    override fun onCleared() {
        super.onCleared()
        log.logStr("onCleared")
        stopRecord()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            kotlin.runCatching { communicationListener?.let { AudioHelper.am.removeOnCommunicationDeviceChangedListener(it) } }
        }
    }

    fun toggleRecord() {
        _toIsRecording = !(_isRecording.value ?: false)
        if (_isRecording.value == true) stopRecord() else startRecord()
    }

    private fun startRecord() {
        log.logStr("startRecord: ${_input?.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO}")
        if (_input?.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {
            startBleSco()
        } else {
            startRecordImpl()
        }
    }

    private fun startRecordImpl() {
        log.logStr("startRecordImpl")
        recordHelper.start()
        _isRecording.postValue(recordHelper.isRecording)
        if (recordHelper.isRecording) {
            viewModelScope.launchIO {
                log.logStr("startRecordLoop")
                kotlin.runCatching { recordLoop() }
                log.logStr("RecordLoop Over")
            }
        }
        viewModelScope.launch(Dispatchers.Main) {
            onStart?.invoke()
        }
    }

    private fun stopRecordImpl() {
        log.logStr("stopRecordImpl")
        recordHelper.stop()
        recordJob?.cancel()
        recordJob = null
        _isRecording.postValue(recordHelper.isRecording)
        onStop?.invoke()
    }

    private fun stopRecord() {
        log.logStr("stopRecord: ${_input?.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO}")
        if (_input?.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {
            stopBleSco()
        } else {
            stopRecordImpl()
        }

    }

    private suspend fun recordLoop() {
        withContext(SupervisorJob().apply { recordJob = this } + Dispatchers.IO) {
            val byte = recordHelper.newBuffer
            while (true) {
                delay(40L)
                val read = recordHelper.record(byte) ?: break
                val db = byte.calculateDB(read)
                _db.emit(db)
            }
        }
    }

    fun setInputDevs(dev: AudioDeviceInfo?) {
        if (dev == _input) return
        _input = dev
    }


    private fun stopBleSco() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (AudioHelper.am.communicationDevice?.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {
                log.logStr("clearCommunicationDevice")
                AudioHelper.am.clearCommunicationDevice()
            }
        }
    }

    private fun startBleSco() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (AudioHelper.am.communicationDevice?.type != AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {
                AudioHelper.am.availableCommunicationDevices.fastFirstOrNull { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO }?.let {
                    log.logStr("setCommunicationDevice: ${it.type}")
                    AudioHelper.am.setCommunicationDevice(it)
                }
            }
        }
    }

    fun setOutputDevs(dev: AudioDeviceInfo?) {
        if (dev == _output) return
        _output = dev
    }
}