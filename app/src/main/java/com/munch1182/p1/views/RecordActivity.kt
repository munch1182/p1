package com.munch1182.p1.views

import android.Manifest
import android.content.Intent
import android.media.AudioFormat
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.core.content.FileProvider
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.munch1182.lib.AppHelper
import com.munch1182.lib.base.Logger
import com.munch1182.lib.base.OnStateValueChangeListener
import com.munch1182.lib.base.asLive
import com.munch1182.lib.base.asStateFlow
import com.munch1182.lib.base.log
import com.munch1182.lib.base.newLog
import com.munch1182.lib.base.nowStr
import com.munch1182.lib.helper.FileHelper
import com.munch1182.lib.helper.FileHelper.sureExists
import com.munch1182.lib.helper.curr
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
import com.munch1182.p1.ui.setContentWithBase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class RecordActivity : AppCompatActivity() {

    private val vm by viewModels<RecordVM>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentWithBase { Views() }
    }

    @Composable
    private fun Views() {
        val isRecording by vm.isRecording.observeAsState(false)
        val path by vm.file.observeAsState(RecordVM.WriteState())

        ClickButton(if (!isRecording) "开始录音" else "停止录音") {
            permission(Manifest.permission.RECORD_AUDIO).dialogWhen(dialogPermission()).manualIntent().request { vm.toggle() }
        }
        Split()
        Value()
        if (!isRecording && path.msg.isNotBlank()) {
            Text(path.msg)
            if (path.path != null) {
                ClickButton("播放") { vm.play(path.path!!) }
                ClickButton("分享") {
                    // Cache路径下的文件不能分享
                    val uri = FileProvider.getUriForFile(curr, "${AppHelper.packageName}.fileprovider", File(path.path!!))
                    val shareIntent = Intent(Intent.ACTION_SEND, uri)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        .setDataAndType(uri, "audio/wav")
                        .putExtra(Intent.EXTRA_STREAM, uri)

                    intent(Intent.createChooser(shareIntent, "分享录音")).request {}
                }
            }
        }
    }

    @Composable
    private fun Value() {
        val db by vm.db.collectAsState()
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
    private val _isRecording = MutableLiveData(false)
    private val _db = MutableStateFlow(0.0)
    private val _file = MutableLiveData(WriteState())

    private val recordHelper = RecordHelper(16000, AudioFormat.CHANNEL_IN_MONO)
    private val audioPlayer by lazy { AudioPlayer(recordHelper.sampleRate) }
    private val fileHelper = RecordWriteHelper(viewModelScope, log)

    val isRecording = _isRecording.asLive()
    val db = _db.asStateFlow()
    val file = _file.asLive()

    private var loopJob: Job? = null

    fun toggle() = if (recordHelper.isRecording) stopRecord() else startRecord()

    private fun startRecord() {
        log.logStr("startRecord")
        fileHelper.prepare(recordHelper)
        recordHelper.start()
        _isRecording.postValue(recordHelper.isRecording)
        runLoop()
    }

    private fun runLoop() {
        if (loopJob?.isActive == true) return
        viewModelScope.launch(Job().apply { loopJob = this } + Dispatchers.IO) {
            while (coroutineContext.isActive) {
                delay(40L)
                val (s, i) = recordHelper.readWithReadValue() ?: continue
                val db = s.calculateDB(i)
                if (db <= 0) continue
                fileHelper.write(s, i)
                _db.emit(db)
            }
        }
    }

    private fun stopRecord() {
        log.logStr("stopRecord")
        stopLoop()
        recordHelper.stop()
        _isRecording.postValue(recordHelper.isRecording)

        fileHelper.stopIfOver()
        fileHelper.collect { it, path ->
            val str = when (it) {
                RecordWriteHelper.GenerateFile -> "正在生成文件"
                RecordWriteHelper.Prepare -> "正在准备"
                RecordWriteHelper.Translate -> "正在转换格式"
                RecordWriteHelper.Write -> "正在写入数据"
                RecordWriteHelper.Complete -> "录音文件已保存：$path"
                RecordWriteHelper.Exeception -> "发生错误"
            }
            _file.postValue(WriteState(str, path))
        }
    }

    private fun stopLoop() {
        loopJob?.cancel()
        loopJob = null
    }

    override fun onCleared() {
        super.onCleared()
        recordHelper.release()
        fileHelper.release()
    }

    fun play(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val fos = FileInputStream(File(path))

            viewModelScope.launch(Dispatchers.IO) { audioPlayer.prepare() }

            val buffer = audioPlayer.buffSize
            val buff = ByteArray(buffer)
            var len = fos.read(buff, 0, buffer)
            while (len >= 0) {
                audioPlayer.write(buff, 0, len)
                len = fos.read(buff, 0, buffer)
            }

            runCatching { fos.close() }
        }
    }

    class WriteState(val msg: String = "", val path: String? = null)

    private class RecordWriteHelper(private val scope: CoroutineScope, log: Logger) {

        private val log = log.newLog("write")

        private var state: State = Prepare
            set(value) {
                if (field == value) return
                log.logStr("state: $field -> $value")
                field = value
                stateChange?.onStateChange(state, currFile?.path)
            }
        private var currFile: File? = null
        private var fos: FileOutputStream? = null
        private var loopJob: Job? = null
        private var channel: Channel<Data>? = null
        private val dir by lazy { FileHelper.newFile("record") } // Cache路径下的文件不能分享
        private var stateChange: OnStateValueChangeListener<State, String?>? = null

        fun prepare(record: RecordHelper) {
            if (loopJob?.isActive == true) return

            log.logStr("prepare()")

            state = Prepare

            //channel.cancel()
            channel = Channel(Channel.UNLIMITED)

            dir.deleteRecursively()
            currFile = File(dir, "${nowStr("yyyyMMddHHmmss")}.pcm").sureExists()
            log.logStr("curr file: $currFile")


            scope.launch(Job().apply { loopJob = this } + Dispatchers.IO) {
                val chan = channel!!
                var size = 0L
                log.logStr("receive start")

                fos = FileOutputStream(currFile!!)

                while (isActive) {
                    val data = chan.receive().takeUnless { it.isEnd }
                    if (data == null) {
                        chan.cancel()
                        break
                    }
                    size += data.size.toLong()
                    state = Write
                    fos?.write(data.byte, 0, data.size) ?: break
                }

                log.logStr("receive over, size: $size")

                kotlin.runCatching { fos?.flush() }
                kotlin.runCatching { fos?.close() }

                fos = null
                realCollect(record, size)
            }
            log.logStr("receive over")
        }

        fun write(byteArray: ByteArray, len: Int = byteArray.size) {
            channel?.trySend(Data(byteArray, len))
        }

        private suspend fun realCollect(record: RecordHelper, dataSize: Long) {
            log.logStr("realCollect()")


            try {
                val wavHeader = translate(record, dataSize)
                generateFile(wavHeader)
            } catch (e: Exception) {
                e.printStackTrace()
                state = Exeception
            } finally {
                loopJob?.cancel()
                loopJob = null
            }
        }

        private fun translate(record: RecordHelper, dataSize: Long): ByteArray {
            log.logStr("translate()")
            state = Translate
            return wavHeader(record, dataSize)
        }

        private suspend fun generateFile(wavHeader: ByteArray) {
            log.logStr("generateFile()")
            state = GenerateFile
            val f = currFile ?: throw IllegalStateException()
            val wav = File(dir, "${f.nameWithoutExtension}_pcm.wav").sureExists()
            log.logStr("wav: $wav")

            withContext(Dispatchers.IO) {
                val pcm = FileInputStream(f)
                val wavFos = FileOutputStream(wav)

                wavFos.write(wavHeader)
                pcm.copyTo(wavFos)

                kotlin.runCatching { wavFos.close() }
                kotlin.runCatching { pcm.close() }

                log.logStr("pac: ${f.length()}, wav: ${wav.length()}")
            }
            //check(f, wav, wavHeader)
            log.logStr("write wav over")
            //currFile = wav
            currFile = f
            state = Complete
            stateChange = null
        }

        fun stopIfOver() {
            channel?.trySend(Data.end())
        }

        fun collect(l: OnStateValueChangeListener<State, String?>) {
            stateChange = l
        }

        fun release() {
            channel?.cancel()
            channel = null
            loopJob?.cancel()
            loopJob = null
            kotlin.runCatching { fos?.close() }
            stateChange = null
            kotlin.runCatching { dir.deleteRecursively() }
        }

        private fun check(from: File, to: File, header: ByteArray) {
            assert(from.length() + header.size == to.length())
            val fis = FileInputStream(from)
            val tis = FileInputStream(to)
            val readHeader = ByteArray(header.size)
            tis.read(readHeader)
            header.forEachIndexed { index, byte -> assert(byte == readHeader[index]) }

            var read = fis.read()
            while (read != -1) {
                assert(read == tis.read())
                read = fis.read()
            }
        }

        sealed class State

        data object Prepare : State()
        data object Write : State()
        data object Translate : State()
        data object GenerateFile : State()
        data object Complete : State()
        data object Exeception : State()

        class Data(val byte: ByteArray, val size: Int = byte.size, val isEnd: Boolean = false) {
            companion object {
                fun end() = Data(byteArrayOf(), 0, true)
            }
        }
    }
}

