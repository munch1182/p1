package com.munch1182.p1.views

import android.Manifest
import android.content.Intent
import android.media.AudioFormat
import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.munch1182.lib.base.asLive
import com.munch1182.lib.base.asStateFlow
import com.munch1182.lib.base.launchIO
import com.munch1182.lib.base.log
import com.munch1182.lib.base.newLog
import com.munch1182.lib.base.nowStr
import com.munch1182.lib.helper.FileHelper
import com.munch1182.lib.helper.FileHelper.sureExists
import com.munch1182.lib.helper.closeQuietly
import com.munch1182.lib.helper.result.PermissionHelper.PermissionCanRequestDialogProvider
import com.munch1182.lib.helper.result.asAllowDenyDialog
import com.munch1182.lib.helper.result.intent
import com.munch1182.lib.helper.result.permission
import com.munch1182.lib.helper.sound.AudioPlayer
import com.munch1182.lib.helper.sound.RecordHelper
import com.munch1182.lib.helper.sound.calculateDB
import com.munch1182.lib.helper.sound.wavHeader
import com.munch1182.p1.base.DialogHelper
import com.munch1182.p1.ui.ClickButton
import com.munch1182.p1.ui.Split
import com.munch1182.p1.ui.setContentWithScroll
import com.munch1182.p1.ui.theme.PagePaddingHalf
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

class RecordActivity : AppCompatActivity() {

    companion object {
        const val SAMPLE = 16000
    }

    private val recordVM by viewModels<RecordVM>()
    private val playVM by viewModels<PlayVM>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentWithScroll { Views() }
    }

    @Composable
    private fun Views() {
        val isRecording by recordVM.isRecording.observeAsState(false)

        ClickButton(if (!isRecording) "开始录音" else "停止录音") {
            permission(Manifest.permission.RECORD_AUDIO).dialogWhen(dialogPermission()).manualIntent().request { recordVM.recordToggle() }
        }
        Split()
        Value()
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
                    .padding(top = PagePaddingHalf, bottom = PagePaddingHalf),
                horizontalArrangement = Arrangement.spacedBy(PagePaddingHalf)
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
        Slider(
            value = progress.progress.toFloat(),
            onValueChange = { playVM.seekPlayTo(it.toInt()) },
            onValueChangeFinished = { playVM.seekPlayTo(-1) },
            valueRange = 0f..progress.max.toFloat()
        )
    }

    private fun share(path: String) {
        // Cache路径下的文件不能分享
        val uri = FileHelper.uri(path)
        val shareIntent = Intent(Intent.ACTION_SEND, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION).setDataAndType(uri, "audio/*").putExtra(Intent.EXTRA_STREAM, uri)

        intent(Intent.createChooser(shareIntent, "分享录音")).request {}
    }

    @Composable
    private fun Value() {
        val db by recordVM.db.collectAsState()
        Text("DB: $db")
    }

    private fun dialogPermission(): PermissionCanRequestDialogProvider {
        return PermissionCanRequestDialogProvider { ctx, state, _ ->
            if (state.isDeniedForever) {
                DialogHelper.newPermissionIntent(ctx, "录音").asAllowDenyDialog()
            } else {
                null
            }
        }
    }
}

class RecordVM : ViewModel() {

    private val log = log()

    // RecordHelper必须在有权限之后创建，所以对其的所有访问都应在有权限之后
    private val recordHelper by lazy { RecordHelper(RecordActivity.SAMPLE, AudioFormat.CHANNEL_IN_MONO) }
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

    private val audioPlayer by lazy { AudioPlayer(RecordActivity.SAMPLE) }
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

