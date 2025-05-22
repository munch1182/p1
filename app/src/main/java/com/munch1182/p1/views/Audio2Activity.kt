package com.munch1182.p1.views

import android.Manifest
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.munch1182.lib.AppHelper
import com.munch1182.lib.base.OnUpdateListener
import com.munch1182.lib.base.asLive
import com.munch1182.lib.base.asStateFlow
import com.munch1182.lib.base.launchIO
import com.munch1182.lib.base.log
import com.munch1182.lib.base.nowStr
import com.munch1182.lib.base.onDestroyed
import com.munch1182.lib.base.toDateStr
import com.munch1182.lib.helper.FileHelper
import com.munch1182.lib.helper.FileWriteHelper
import com.munch1182.lib.helper.result.onGranted
import com.munch1182.lib.helper.result.permission
import com.munch1182.lib.helper.sound.AudioHelper
import com.munch1182.lib.helper.sound.AudioPlayer
import com.munch1182.lib.helper.sound.RecordHelper
import com.munch1182.lib.helper.sound.calculateDB
import com.munch1182.p1.R
import com.munch1182.p1.base.BaseActivity
import com.munch1182.p1.base.DialogHelper
import com.munch1182.p1.base.permissionDialog
import com.munch1182.p1.base.show
import com.munch1182.p1.base.toast
import com.munch1182.p1.ui.ClickButton
import com.munch1182.p1.ui.ComposeListView
import com.munch1182.p1.ui.ComposeView
import com.munch1182.p1.ui.DownUpArrow
import com.munch1182.p1.ui.EmptyMsg
import com.munch1182.p1.ui.Split
import com.munch1182.p1.ui.StateButton
import com.munch1182.p1.ui.setContentWithRv
import com.munch1182.p1.ui.theme.FontDescSize
import com.munch1182.p1.ui.theme.PagePadding
import com.munch1182.p1.ui.theme.PagePaddingHalf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.io.File

class Audio2Activity : BaseActivity() {

    private val audioVM by viewModels<Audio2VM>()
    private val recordVM by viewModels<Record2VM>()
    private val recordHandleVM by viewModels<RecordHandleVM>()
    private val playVM by viewModels<AudioPlayVM>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentWithRv { Views() }
        launchIO {
            recordVM.recordData.collect {
                recordHandleVM.recordData(it)
                recordHandleVM.writeFile(it)
            }
        }
        launchIO {
            audioVM.mediaButton.collect { if (it?.isPlayOrPause() == true) recordVM.startRecordOrStop() }
        }
    }

    @Composable
    private fun Views() {
        Operate()
        Split()
        Devices()
        Split()
        Record()
        Split()
        RecordHandle()
    }

    @Composable
    private fun Devices() {
        var inputType by remember { mutableIntStateOf(-1) }
        var outputType by remember { mutableIntStateOf(-1) }

        MsgButton("已选中 ${inputType.toAudiTypeStr()}", inputType != -1) {
            ClickButton("输入设备") {
                showSelectDevicesView(audioVM.collectInputDevices(), null) {
                    inputType = it?.type ?: -1
                    recordVM.setPreferredDevice(it)
                }
            }
        }
        MsgButton("已选中 ${outputType.toAudiTypeStr()}", outputType != -1) {
            ClickButton("输出设备") {
                showSelectDevicesView(audioVM.collectOutputDevices(), null) {
                    outputType = it?.type ?: -1
                    recordVM.setPreferredDevice(it)
                }
            }
        }
    }

    @Composable
    private fun Operate() {
        val isFocus by audioVM.focus.observeAsState(false)
        val isListen by audioVM.isListen.observeAsState(false)
        var showMbv by remember { mutableStateOf(false) }
        StateButton(if (isFocus) "清除当前焦点" else "获取音频焦点", isFocus) { audioVM.gainFocusOrNot(!isFocus) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            StateButton(if (isListen) "取消按键监听" else "监听媒体按键", isListen) { audioVM.listenButtonOrNot(!isListen) }
            Spacer(Modifier.width(PagePadding))
            if (isListen) DownUpArrow(showMbv) { showListenMediaButtonView { showMbv = it } }
        }

    }

    @Composable
    private fun Record() {
        val isRecording by recordVM.isRecording.observeAsState(false)
        val isScoOn by recordVM.blueScoIsOpen.observeAsState(false)
        MsgButton("Bluetooth Sco录音已开启", isScoOn) {
            StateButton(if (isRecording) "停止录音" else "开始录音", isRecording) {
                permission(Manifest.permission.RECORD_AUDIO).permissionDialog("录音", "录音").manualIntent().onGranted {
                    recordHandleVM.startRecordOrStop(!isRecording)
                    recordVM.startRecordOrStop()
                }
            }
        }
    }

    @Composable
    private fun RecordHandle() {
        val db by recordHandleVM.db.collectAsState()
        val file by recordHandleVM.file.observeAsState()

        Text("DB: $db")

        val f = file
        if (f != null) {
            Split()
            Text("录音文件：$file")
            Play(f)
        }
    }

    @Composable
    private fun Play(path: String) {
        ClickButton("播放") { playVM.play(path) }
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
                        Text(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) "(${dev.address}, ${dev.productName})" else "(${dev.productName})")
                        Text(if (dev.sampleRates.isEmpty() || dev.sampleRates.size == 9) "*" else dev.sampleRates.joinToString("/"))
                        Text(if (dev.channelMasks.isEmpty()) "*" else dev.channelMasks.joinToString { i -> i.toChannelStr() })
                        Text(if (dev.encodings.isEmpty()) "*" else dev.encodings.joinToString { i -> i.toEncodingStr() })
                        if (routed?.type != null && dev.type == routed.type) Text("ROUTED")
                    }
                    HorizontalDivider()
                }
            }
        }.show()
    }

    private fun showListenMediaButtonView(showOrNot: (Boolean) -> Unit) {
        val list = mutableListOf<Pair<KeyEvent, Long>>()
        DialogHelper.newBottom { ctx, _ ->
            ComposeView(ctx) {
                val btn by audioVM.mediaButton.collectAsState()
                btn?.let { list.add(it to System.currentTimeMillis()) }

                EmptyMsg(list.isEmpty(), "当前未接收到按键") {
                    LazyColumn(modifier = Modifier.defaultMinSize(minHeight = 200.dp)) {
                        items(list.size) { i ->
                            val (it, time) = list[i]
                            Text("${it.toStr()} ${time.toDateStr("mm:ss.sss")}", modifier = Modifier.padding(horizontal = PagePadding, vertical = PagePaddingHalf))
                        }
                    }
                }
            }
        }.apply { lifecycle.onDestroyed { showOrNot.invoke(false) } }.show()
        showOrNot.invoke(true)
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
            else -> this.toString()
        }
    }

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

            else -> this.toString()
        }
        return "$name($this)"
    }

    @Composable
    private fun MsgButton(msg: String, show: Boolean = true, button: @Composable () -> Unit) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            button()
            if (show) {
                Spacer(modifier = Modifier.width(PagePadding))
                Text(msg, fontSize = FontDescSize)
            }
        }
    }

    private fun KeyEvent.isPlayOrPause(): Boolean {
        if (this.keyCode == KeyEvent.KEYCODE_HEADSETHOOK || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
            if (this.action == KeyEvent.ACTION_UP) {
                return true
            }
        }
        if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
            if (this.action == KeyEvent.ACTION_DOWN) {
                return true
            }
        }
        return false
    }
}

class Audio2VM : ViewModel() {
    private val log = log()
    private val _focus = MutableLiveData(false)
    private val _mediaButton = MutableStateFlow<KeyEvent?>(null)
    private val _isListen = MutableLiveData(false)

    private val focusHelper = AudioHelper.FocusHelper().setFocusGain(AudioManager.AUDIOFOCUS_GAIN).apply {
        add {
            when (it) {
                AudioManager.AUDIOFOCUS_GAIN -> {
                    log.logStr("Audio Focus Gain")
                    _focus.postValue(true)
                }

                AudioManager.AUDIOFOCUS_LOSS -> {
                    log.logStr("Audio Focus Loss")
                    _focus.postValue(false)
                }

                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    log.logStr("Audio Focus Loss Transient")
                    _focus.postValue(false)
                }
            }
        }
    }
    private val listenHelper = AudioHelper.MediaButtonHelper().apply {
        add {
            log.logStr("按键：${it.keyCode}, ${it.action}")
            viewModelScope.launchIO { _mediaButton.emit(it) }
        }
    }
    private val fakePlayer by lazy { fakePlay() }

    val focus = _focus.asLive()
    val mediaButton = _mediaButton.asStateFlow()
    val isListen = _isListen.asLive()

    override fun onCleared() {
        super.onCleared()
        focusHelper.release()
        listenHelper.release()
        fakePlayer.release()
    }

    fun gainFocusOrNot(focus: Boolean) {
        val result = if (focus) {
            focusHelper.requestAudioFocus()
        } else {
            focusHelper.clearAudioFocus()
        }
        if (result) _focus.postValue(focus)
        log.logStr("gainFocusOrNot: $focus: $result")
    }

    fun listenButtonOrNot(listen: Boolean) {
        if (listen) {
            fakePlayer.start()
            fakePlayer.pause()
            listenHelper.listen()
        } else {
            listenHelper.unListen()
        }
        _isListen.postValue(listen)
        log.logStr("listenButtonOrNot: $listen")
    }

    private fun fakePlay() = MediaPlayer.create(AppHelper, R.raw.lapple).apply { isLooping = true }

    fun collectInputDevices(): Array<out AudioDeviceInfo>? {
        return AudioHelper.am.getDevices(AudioManager.GET_DEVICES_INPUTS)
    }

    fun collectOutputDevices(): Array<out AudioDeviceInfo>? {
        return AudioHelper.am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
    }
}

class Record2VM : ViewModel() {
    private val log = log()
    private var recordHelper: RecordHelper? = null
    private var recordLoopJob: Job? = null
    private val scoHelper by lazy { AudioHelper.BlueScoHelper() }
    private var preferredDevice: AudioDeviceInfo? = null
    private val scoListener = OnUpdateListener<Boolean> {
        log.logStr("scoState: $it")
        _blueScoIsOpen.postValue(it)
        if (it) {
            startRecordImpl()
        } else { // 开启sco后点击播放/暂停按键可挂断
            stopRecord()
        }
    }

    private var _blueScoIsOpen = MutableLiveData(false)
    private var _isRecording = MutableLiveData(false)
    private var _recordData = MutableStateFlow(byteArrayOf())

    val isRecording = _isRecording.asLive()
    val recordData = _recordData.asStateFlow()
    val blueScoIsOpen = _blueScoIsOpen.asLive()

    init {
        scoHelper.setBlueScoConnectListener(l = scoListener)
    }

    fun setPreferredDevice(dev: AudioDeviceInfo?) {
        this.preferredDevice = dev
    }

    fun startRecordOrStop() {
        if (_isRecording.value == true) stopRecord() else startRecord()
    }

    fun startRecord() {
        val isScoOn = scoHelper.isBlueScoOn()
        val choseSco = preferredDevice?.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
        log.logStr("startRecord: setSco: ${choseSco}, isScoOn: $isScoOn")
        // 如果选择蓝牙录音且sco未开启
        if (choseSco && !isScoOn) {
            // 则开启sco并等待开启完成再开始录音
            scoHelper.startBlueSco()
            // 如果不是sco但sco已开启
        } else if (!choseSco && isScoOn) {
            // 则先关闭sco并等待关闭完成再开始录音
            scoHelper.stopBlueSco()
            // 否则开始录音
        } else {
            startRecordImpl()
        }
    }

    private fun startRecordImpl() {
        val isRecording = newRecordHelperIfNeedOrThe().start()
        _isRecording.postValue(isRecording)
        log.logStr("startRecordImpl: $isRecording")
        if (isRecording) {
            viewModelScope.launchIO {
                log.logStr("startRecordLoop")
                kotlin.runCatching { recordLoop() }
                log.logStr("stopRecordLoop")

                recordLoopJob = null
                _isRecording.postValue(false)
            }
        }
    }

    private suspend fun recordLoop() {
        withContext(SupervisorJob().apply { recordLoopJob = this } + Dispatchers.IO) {
            val helper = recordHelper ?: return@withContext
            val buffer = helper.newBuffer
            while (true) {
                delay(40L)
                val read = helper.record(buffer) ?: continue
                _recordData.emit(buffer.copyOf(read))
            }
        }
    }

    fun stopRecord() {
        log.logStr("stopRecord")
        recordHelper?.stop()
        recordLoopJob?.cancel()
    }

    // 如果传入的dev与当前使用的参数不一致，则重建recordHelper，否则直接使用当前recordHelper
    // 传入此处的dev不会作为参数去调用setPreferredDevice
    private fun newRecordHelperIfNeedOrThe(): RecordHelper {
        val needNewRecord = recordHelper?.preferredDevice?.let { it.id == preferredDevice?.id } ?: false
        log.logStr("newRecordHelperIfNeedOrThe: needNewRecord: $needNewRecord")
        if (needNewRecord) releaseRecordHelper()
        return theOrNewRecordHelper()
    }

    // 如果recordHelper为空，则创建新的recordHelper，否则直接返回recordHelper
    private fun theOrNewRecordHelper(): RecordHelper {
        val dev = preferredDevice
        if (recordHelper == null) {
            log.logStr("newRecordHelper")
            val sample = dev?.sampleRates?.getOrNull(0) ?: 16000
            val channel = dev?.channelMasks?.getOrNull(0) ?: AudioFormat.CHANNEL_IN_MONO
            val format = dev?.encodings?.getOrNull(0) ?: AudioFormat.ENCODING_PCM_16BIT
            recordHelper = RecordHelper(sample, channel, format)
        }
        if (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) recordHelper?.preferredDevice?.address != dev?.address else recordHelper?.preferredDevice?.id != dev?.id) {
            val res = recordHelper?.setPreferredDevice(dev)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                log.logStr("setPreferredDevice: ${dev?.address}(${recordHelper?.preferredDevice?.address}/${dev?.address}): $res")
            } else {
                log.logStr("setPreferredDevice: ${dev}(${recordHelper?.preferredDevice?.id}/${dev?.id}): $res")
            }
        }
        return recordHelper!!

    }

    private fun releaseRecordHelper() {
        log.logStr("releaseRecordHelper")
        recordHelper?.release()
        recordHelper = null
    }
}

class RecordHandleVM : ViewModel() {

    private val writeHelper by lazy { FileWriteHelper() }

    private val _db = MutableStateFlow(0.0)
    private val _file = MutableLiveData<String?>()

    val db = _db.asStateFlow()
    val file = _file.asLive()

    suspend fun recordData(data: ByteArray) {
        val db = data.calculateDB(data.size)
        _db.emit(db)
    }

    fun startRecordOrStop(isStart: Boolean) {
        if (isStart) {
            writeHelper.prepare(FileHelper.newCache("record", "${nowStr("yyyy_MM_dd_HH_mm_ss")}.pcm"), true)
        } else {
            _file.postValue(writeHelper.complete()?.absolutePath ?: "null")
        }
    }

    fun writeFile(data: ByteArray) {
        writeHelper.write(data)
    }
}

class AudioPlayVM : ViewModel() {

    private val play by lazy { AudioPlayer() }

    fun play(path: String) {
        play.prepare()
        val buffer = play.newBuffer
        File(path).inputStream().use {
            while (it.read(buffer) != -1) {
                play.write(buffer)
            }
            play.writeOver()
        }
    }
}
