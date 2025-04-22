package com.munch1182.p1.views

import android.Manifest
import android.content.Intent
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.munch1182.lib.AppHelper
import com.munch1182.lib.base.Logger
import com.munch1182.lib.base.ThreadHelper
import com.munch1182.lib.base.asLive
import com.munch1182.lib.base.asStateFlow
import com.munch1182.lib.base.launchIO
import com.munch1182.lib.base.log
import com.munch1182.lib.base.newLog
import com.munch1182.lib.base.nowStr
import com.munch1182.lib.helper.FileHelper
import com.munch1182.lib.helper.FileHelper.sureExists
import com.munch1182.lib.helper.closeQuietly
import com.munch1182.lib.helper.result.intent
import com.munch1182.lib.helper.result.permission
import com.munch1182.lib.helper.sound.AudioHelper
import com.munch1182.lib.helper.sound.AudioPlayer
import com.munch1182.lib.helper.sound.RecordHelper
import com.munch1182.lib.helper.sound.calculateDB
import com.munch1182.lib.helper.sound.wavHeader
import com.munch1182.p1.R
import com.munch1182.p1.base.handlePermissionWithName
import com.munch1182.p1.ui.ClickButton
import com.munch1182.p1.ui.DescText
import com.munch1182.p1.ui.Split
import com.munch1182.p1.ui.setContentWithScroll
import com.munch1182.p1.ui.theme.PagePadding
import com.munch1182.p1.ui.theme.PagePaddingHalf
import com.munch1182.p1.views.AudioVM.InputType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.ArrayDeque

class AudioActivity : AppCompatActivity() {

    companion object {
        const val SAMPLE = 16000
    }

    private val audioVM by viewModels<AudioVM>()
    private val recordVM by viewModels<RecordVM>()
    private val playVM by viewModels<PlayVM>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentWithScroll { Views() }
    }

    @Composable
    private fun Views() {
        Column {
            Operate()
            Split()
            Record()
        }
    }

    @Composable
    private fun Operate() {
        val focus by audioVM.focus.observeAsState(false)
        val listen by audioVM.listen.observeAsState(false)
        val input by audioVM.input.observeAsState(InputType.Phone)
        val keys by audioVM.keys.collectAsState(arrayOf())
        Row {
            Column {
                ClickButton(if (focus) "清除音频焦点" else "获取音频焦点") { audioVM.toggleFocus() }
                ClickButton(if (listen) "清除按键监听" else "监听媒体按键") { audioVM.toggleMediaButtonListen() }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ClickButton("当前输入：${if (input.isPhone) "手机" else "耳机"}") { audioVM.changeAudioInput() }
                }
            }
            Spacer(Modifier.width(PagePadding * 2))
            Column {
                LazyColumn { items(keys.size) { DescText(keys[it]) } }
            }
        }
    }

    @Composable
    private fun Record() {
        val isRecording by recordVM.isRecording.observeAsState(false)

        ClickButton(if (!isRecording) "开始录音" else "停止录音") {
            permission(Manifest.permission.RECORD_AUDIO).handlePermissionWithName("录音").request {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) audioVM.sureInput()
                recordVM.recordToggle()
            }
        }
        Split()
        Value()
        Split()
        if (!isRecording) Files()
    }

    @Composable
    private fun Files() {
        val state by recordVM.file.observeAsState(RecordVM.WriteState())
        val list = state.toList()
        LazyColumn { items(list.size) { index -> Item(list[index]) } }
    }

    @Composable
    private fun Item(item: RecordVM.WriteState) {
        val isPlay by playVM.isPlay.observeAsState(false)
        Text(item.msg)
        item.path?.let { p ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = PagePaddingHalf, bottom = PagePaddingHalf), horizontalArrangement = Arrangement.spacedBy(PagePaddingHalf)
            ) {
                ClickButton(if (isPlay) "停止" else "播放") { if (isPlay) playVM.stopPlay() else playVM.playByAudioTrack(p) }
                if (item.isPcm) ClickButton("转换") { recordVM.trans(p) }
                if (item.isWav) ClickButton("播放") { playVM.playByMedia(p) }
                ClickButton("分享") { share(p) }
            }
            if (item.isWav && isPlay) ProgressPlay()
        }
    }

    @Composable
    private fun ProgressPlay() {
        val progress by playVM.progressPlay.collectAsState(PlayVM.Progress(0, 1))
        Slider(value = progress.progress.toFloat(), onValueChange = { playVM.seekPlayTo(it.toInt()) }, onValueChangeFinished = { playVM.seekPlayTo(-1) }, valueRange = 0f..progress.max.toFloat()
        )
    }

    @Composable
    private fun Value() {
        val db by recordVM.db.collectAsState()
        Text("DB: $db")
    }

    private fun share(path: String) {
        // Cache路径下的文件不能分享
        val uri = FileHelper.uri(path)
        val shareIntent = Intent(Intent.ACTION_SEND, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION).setDataAndType(uri, "audio/*").putExtra(Intent.EXTRA_STREAM, uri)

        intent(Intent.createChooser(shareIntent, "分享录音")).request {}
    }
}

class AudioVM : ViewModel() {
    private val log = log()
    private val keysList = ArrayDeque<String>(7)
    private val focusHelper = AudioHelper.FocusHelper().setFocusGain(AudioManager.AUDIOFOCUS_GAIN)
    private val listenHelper = AudioHelper.MediaButtonHelper().apply {
        add {
            if (keysList.size == 6) keysList.removeFirst()

            val action = when (it.action) {
                KeyEvent.ACTION_DOWN -> "按下"
                KeyEvent.ACTION_UP -> "抬起"
                else -> it.action.toString()
            }
            val name = when (it.keyCode) {
                KeyEvent.KEYCODE_MEDIA_PLAY -> "播放"
                KeyEvent.KEYCODE_MEDIA_PAUSE -> "暂停"
                KeyEvent.KEYCODE_MEDIA_NEXT -> "下一首"
                KeyEvent.KEYCODE_MEDIA_PREVIOUS -> "上一首"
                else -> it.keyCode.toString()
            }
            log.logStr("按键：$name, $action")
            keysList.add("$name $action")
            viewModelScope.launchIO { _keys.emit(keysList.toTypedArray()) }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private val inputHelper = AudioHelper.InputHelper()

    private var currInput: InputType = InputType.Phone

    private val _focus = MutableLiveData(false)
    private val _listen = MutableLiveData(false)
    private val _input = MutableLiveData(currInput)
    private val _keys = MutableStateFlow(arrayOf<String>())

    private var player: MediaPlayer? = null

    @RequiresApi(Build.VERSION_CODES.S)
    private val inputListener = AudioManager.OnCommunicationDeviceChangedListener {
        log.logStr("OnCommunicationDeviceChanged: curr: ${it?.toStr()}")
    }

    val focus = _focus.asLive()
    val listen = _listen.asLive()
    val input = _input.asLive()
    val keys = _keys.asStateFlow()

    init {
        focusHelper.add {
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            inputHelper.listenChanged(ThreadHelper.cacheExecutor, inputListener)
        }
    }

    private fun gainAudioFocus() {
        val res = focusHelper.requestAudioFocus()
        log.logStr("gainAudioFocus: $res")
        _focus.postValue(res)
    }

    private fun clearAudioFocus() {
        val res = focusHelper.clearAudioFocus()
        log.logStr("clearAudioFocus: $res")
        _focus.postValue(!res)
    }

    fun toggleFocus() = if (_focus.value == true) clearAudioFocus() else gainAudioFocus()

    fun toggleMediaButtonListen() = if (_listen.value == true) unListenMediaButton() else listenMediaButton()

    private fun listenMediaButton() {
        val player = player ?: fakePlay().apply { player = this }
        player.start()
        player.pause()
        listenHelper.listen()
        log.logStr("listenMediaButton")
        _listen.postValue(true)
    }

    private fun fakePlay() = MediaPlayer.create(AppHelper, R.raw.lapple).apply { isLooping = true }

    private fun unListenMediaButton() {
        runBlocking { _keys.emit(arrayOf()) }
        player?.release()
        player = null
        listenHelper.release()
        log.logStr("unListenMediaButton")
        _listen.postValue(false)
    }

    override fun onCleared() {
        super.onCleared()
        focusHelper.clear()
        listenHelper.release()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            inputHelper.unListenChanged(inputListener)
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun sureInput() = setInput(currInput)

    @RequiresApi(Build.VERSION_CODES.S)
    private fun setInput(type: InputType) {
        log.logStr("setInput: $currInput -> $type")
        val sysType = type.type
        if (inputHelper.currRecordFrom() == sysType) return
        log.logStr("setInput: $type")
        val result = inputHelper.setRecordFrom(sysType) ?: false
        log.logStr("setInput: $type result: $result")
        if (!result) return
        currInput = type
        _input.postValue(type)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun changeAudioInput() {
        logAudioDeviceInfo()
        setInput(currInput.next)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun logAudioDeviceInfo() {
        val ava = AudioHelper.am.availableCommunicationDevices
        val curr = AudioHelper.am.communicationDevice
        log.logStr("available: ${ava.map { it.toStr() }}")
        log.logStr("curr: ${curr?.toStr()}")
    }

    private fun AudioDeviceInfo.toStr() = "${productName}(id:${id}, type:${type})"

    sealed class InputType {
        data object Phone : InputType()
        data object Blue : InputType()

        val isPhone get() = this is Phone
        val isBlue get() = this is Blue

        val next
            get() = when (this) {
                is Phone -> Blue
                is Blue -> Phone
            }

        val type: Int
            get() = when (this) {
                Blue -> AudioDeviceInfo.TYPE_BLUETOOTH_SCO
                Phone -> AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
            }
    }

}

class RecordVM : ViewModel() {

    private val log = log()

    // RecordHelper必须在有权限之后创建，所以对其的所有访问都应在有权限之后
    private val recordHelper by lazy { RecordHelper(MediaRecorder.AudioSource.VOICE_COMMUNICATION, AudioActivity.SAMPLE, AudioFormat.CHANNEL_IN_MONO) }
    private val writeHelper by lazy { RecordWriteHelper(log) }

    private val _isRecording = MutableLiveData(false)
    private val _db = MutableStateFlow(0.0)
    private val _file = MutableLiveData(WriteState())

    val isRecording = _isRecording.asLive()
    val db = _db.asStateFlow()
    val file = _file.asLive()

    private val _isRecordingImpl get() = recordHelper.isRecording
    private var recordJob: Job? = null
    private var tranJob: Job? = null

    fun recordToggle() = if (_isRecordingImpl) stopRecord() else startRecord()

    private fun startRecord() {
        log.logStr("startRecord")
        recordHelper.start()
        _isRecording.postValue(recordHelper.isRecording)
        viewModelScope.launchIO {
            writeHelper.prepare()
            _file.postValue(WriteState.pcmPrepare())

            log.logStr("startRecordLoop")
            kotlin.runCatching { recordLoop() }
            log.logStr("RecordLoop Over")

            recordJob = null
            _isRecording.postValue(false)

            val f = writeHelper.complete()
            _file.postValue(WriteState.pcmComplete(f.path))
        }
    }

    private suspend fun recordLoop() {
        withContext(SupervisorJob().apply { recordJob = this } + Dispatchers.IO) {
            val byte = recordHelper.newBuffer
            while (true) {
                delay(100L)
                val read = recordHelper.record(byte) ?: break
                val db = byte.calculateDB(read)
                writeHelper.write(byte, read)
                _db.emit(db)
            }
        }
    }

    private fun stopRecord() {
        log.logStr("stopRecord")
        recordHelper.stop()
        recordJob?.cancel()
        recordJob = null
        _isRecording.postValue(recordHelper.isRecording)
    }

    fun trans(path: String) {
        viewModelScope.launchIO {
            writeHelper.prepareWav(path)
            _file.postValue(WriteState.pcmComplete(path).wavPrepare())

            log.logStr("startTransWavLoop")
            kotlin.runCatching { tranWavLoop(path) }
            log.logStr("TransWavLoop Over")

            tranJob = null

            val f = writeHelper.complete()
            _file.postValue(WriteState.pcmComplete(path).wavComplete(f.path))
        }
    }

    private suspend fun tranWavLoop(path: String) {
        withContext(SupervisorJob().apply { tranJob = this } + Dispatchers.IO) {
            val pcm = File(path)
            val dataSize = pcm.length()

            val fow = FileInputStream(pcm)

            val head = recordHelper.wavHeader(dataSize)
            writeHelper.write(head)

            val byte = recordHelper.newBuffer
            var read = fow.read(byte)
            while (read != -1) {
                writeHelper.write(byte, read)
                read = fow.read(byte)
            }

            fow.closeQuietly()
        }
    }

    private fun stopTrans() {
        tranJob?.cancel()
        tranJob = null
    }

    class ItemState(val isPlaying: Boolean = false, val writeState: WriteState = WriteState()) {
        fun togglePlaying() = ItemState(!isPlaying, writeState)
    }

    class WriteState(private val type: Type = PCM, val msg: String = "", val path: String? = null, private val _writeState: HashMap<Type, WriteState> = hashMapOf()) {
        companion object {
            fun pcmPrepare() = WriteState(PCM, "准备文件中")
            fun pcmComplete(path: String) = WriteState(PCM, "文件已保存到$path", path)
        }

        fun wavPrepare(): WriteState {
            this._writeState[WAV] = WriteState(WAV, "准备文件中")
            return this
        }

        fun wavComplete(path: String): WriteState {
            this._writeState[WAV] = WriteState(WAV, "文件已保存到$path", path)
            return this
        }

        sealed class Type
        data object PCM : Type()
        data object WAV : Type()

        fun toList(): MutableList<WriteState> {
            val list = mutableListOf<WriteState>()
            list.add(this)
            if (_writeState.isNotEmpty()) {
                _writeState.forEach { list.add(it.value) }
            }
            return list
        }

        val isPcm get() = type is PCM
        val isWav get() = type is WAV
    }

    override fun onCleared() {
        super.onCleared()
        stopRecord()
        stopTrans()
        writeHelper.clear()
    }

    class RecordWriteHelper(from: Logger) {

        private val log = from.newLog("write")

        private val dir = FileHelper.newFile("record")
        private var fos: FileOutputStream? = null
        private var file: File? = null

        fun write(byte: ByteArray, read: Int = byte.size) {
            fos?.write(byte, 0, read)
        }

        fun prepare() {
            log.logStr("prepare")
            clear()
            prepareImpl(File(dir, "${nowStr("yyyyMMddHHmmss")}.pcm"))
        }

        fun prepareWav(pcm: String) {
            val f = File(pcm)
            if (!f.exists()) throw IllegalStateException()
            val name = f.nameWithoutExtension
            val wav = File(dir, "${name}_pcm.wav")
            prepareImpl(wav)
        }

        private fun prepareImpl(file: File) {
            this.file = file.sureExists()
            fos = FileOutputStream(file)
        }

        fun complete(): File {
            log.logStr("complete")
            fos?.closeQuietly()
            fos = null
            return file ?: throw IllegalStateException()
        }

        fun clear() {
            log.logStr("clear")
            fos?.closeQuietly()
            fos = null
            file = null
            dir.deleteRecursively()
        }
    }
}

class PlayVM : ViewModel() {

    private val _isPlay = MutableLiveData(false)
    private val _progressPlay = MutableStateFlow(Progress(0, 0))

    val isPlay = _isPlay.asLive()
    val progressPlay = _progressPlay.asStateFlow()

    private val audioPlayer by lazy { AudioPlayer(AudioActivity.SAMPLE) }
    private val log = log()
    private var stopPlay = false

    private var media: MediaPlayer? = null

    fun stopPlay() {
        log.logStr("stopPlay")
        stopPlay = true
        audioPlayer.stop()

        if (media?.isPlaying == true) {
            media?.stop()
        }
    }

    fun playByAudioTrack(path: String) {
        log.logStr("playByAudioTrack $path")
        audioPlayer.prepare()
        _isPlay.postValue(true)
        viewModelScope.launchIO {
            val f = FileInputStream(File(path))
            val buffer = audioPlayer.newBuffer
            var read = f.read(buffer)
            log.logStr("loop write start")
            while (read != -1 && !stopPlay) {
                audioPlayer.write(buffer, 0, read)
                read = f.read(buffer)
            }
            log.logStr("loop write over")
            if (!stopPlay) {
                audioPlayer.writeOver {
                    log.logStr("writeOver callback")
                    _isPlay.postValue(false)
                }
            } else {
                _isPlay.postValue(false)
            }
            stopPlay = false
            f.closeQuietly()
        }
    }

    fun seekPlayTo(it: Int) {
        if (it == -1) {
            media?.seekTo(_progressPlay.value.progress)
        } else {
            runBlocking { _progressPlay.emit(Progress(it, _progressPlay.value.max)) }
        }
    }

    fun playByMedia(path: String) {
        val media = MediaPlayer.create(AppHelper, FileHelper.uri(path))
        media.setOnCompletionListener { _isPlay.postValue(false) }
        media.start()
        viewModelScope.launchIO {
            _progressPlay.emit(Progress(0, media.duration))
            while (media.isPlaying) {
                delay(1000L)
                _progressPlay.emit(Progress(media.currentPosition, media.duration))
            }
        }
        _isPlay.postValue(true)
        this.media = media
    }

    private fun release() {
        media?.let {
            it.stop()
            it.release()
        }
        media = null
    }

    override fun onCleared() {
        super.onCleared()
        release()
        stopPlay()
    }

    class Progress(val progress: Int, val max: Int)
}

