package com.munch1182.p1.views

import android.Manifest
import android.content.Intent
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
import com.munch1182.lib.base.OnStateValueChangeListener
import com.munch1182.lib.base.asLive
import com.munch1182.lib.base.asStateFlow
import com.munch1182.lib.base.nowStr
import com.munch1182.lib.helper.FileHelper
import com.munch1182.lib.helper.FileHelper.sureExists
import com.munch1182.lib.helper.RecordHelper
import com.munch1182.lib.helper.calculateDB
import com.munch1182.lib.helper.curr
import com.munch1182.lib.helper.result.PermissionHelper.PermissionCanRequestDialogProvider
import com.munch1182.lib.helper.result.asAllowDenyDialog
import com.munch1182.lib.helper.result.intent
import com.munch1182.lib.helper.result.permission
import com.munch1182.p1.base.DialogHelper
import com.munch1182.p1.ui.ClickButton
import com.munch1182.p1.ui.Split
import com.munch1182.p1.ui.setContentWithBase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.LinkedBlockingQueue

class RecordActivity : AppCompatActivity() {

    private val vm by viewModels<RecordVM>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentWithBase { Views() }
    }

    @Composable
    private fun Views() {
        val isRecording by vm.isRecording.observeAsState(false)
        val path by vm.file.observeAsState()
        ClickButton(if (!isRecording) "开始录音" else "停止录音") {
            permission(Manifest.permission.RECORD_AUDIO).dialogWhen(dialogPermission()).manualIntent().request { vm.toggle() }
        }
        Split()
        Value()
        if (!isRecording && !path.isNullOrEmpty()) {
            Text("录音文件已保存: $path，点击查看")
            ClickButton("分享") {
                val uri = FileProvider.getUriForFile(curr, "${AppHelper.packageName}.fileprovider", File(path!!))
                val shareIntent = Intent(Intent.ACTION_SEND, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION).setDataAndType(uri, "audio/*")

                intent(Intent.createChooser(shareIntent, "分享录音")).request {}
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
    private val _isRecording = MutableLiveData(false)
    private val _db = MutableStateFlow(0.0)
    private val _file = MutableLiveData("")

    private val recordHelper = RecordHelper()
    private val fileHelper = RecordWriteHelper(viewModelScope)

    val isRecording = _isRecording.asLive()
    val db = _db.asStateFlow()
    val file = _file.asLive()

    private var loopJob: Job? = null

    fun toggle() = if (recordHelper.isRecording) stopRecord() else startRecord()

    private fun startRecord() {
        fileHelper.prepare()
        recordHelper.start()
        _isRecording.postValue(recordHelper.isRecording)
        runLoop()
    }

    private fun runLoop() {
        if (loopJob?.isActive == true) return
        viewModelScope.launch(Job().apply { loopJob = this } + Dispatchers.IO) {
            while (coroutineContext.isActive) {
                delay(100L)
                val (s, i) = recordHelper.readWithReadValue() ?: continue
                val db = s.calculateDB(i)
                fileHelper.write(s, i)
                _db.emit(db)
            }
        }
    }

    private fun stopRecord() {
        stopLoop()
        recordHelper.stop()
        _isRecording.postValue(recordHelper.isRecording)

        fileHelper.stop()
        fileHelper.collect { it, path ->
            val str = when (it) {
                RecordWriteHelper.GenerateFile -> "正在生成文件"
                RecordWriteHelper.Prepare -> "正在准备"
                RecordWriteHelper.Translate -> "正在转换格式"
                RecordWriteHelper.Write -> "正在写入数据"
                RecordWriteHelper.Complete -> "录音文件已保存：$path"
            }
            _file.postValue(str)
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

    class RecordWriteHelper(private val scope: CoroutineScope) {

        private var state: State = Prepare
            set(value) {
                field = value
                stateChange?.onStateChange(state, currFile?.path)
            }
        private var currFile: File? = null
        private var fos: FileOutputStream? = null
        private var loopJob: Job? = null
        private val queue = LinkedBlockingQueue<Data>()
        private val dir by lazy { FileHelper.newCache("record") }
        private var stateChange: OnStateValueChangeListener<State, String?>? = null

        fun prepare() {
            if (loopJob?.isActive == true) return
            state = Prepare
            queue.clear()

            dir.delete()
            currFile = File(dir, "${nowStr("yyyyMMddHHmmss")}.pcm").apply { sureExists() }

            fos = FileOutputStream(currFile!!)

            scope.launch(Job().apply { loopJob = this } + Dispatchers.IO) {
                while (isActive) {
                    val data = queue.take()
                    if (data.isEnd) break

                    state = Write
                    fos?.write(data.byte, 0, data.size) ?: break
                }
                realCollect()
            }
        }

        fun write(byteArray: ByteArray, len: Int = byteArray.size) {
            queue.put(Data(byteArray, len))
        }

        private fun realCollect() {
            kotlin.runCatching { fos?.flush() }
            kotlin.runCatching { fos?.close() }
            fos = null
            loopJob?.cancel()
            loopJob = null

            translate()
            generateFile()
        }

        private fun translate() {
            state = Translate
        }

        private fun generateFile() {
            state = GenerateFile
        }

        fun stop() {
            queue.put(Data.end())
        }

        fun collect(l: OnStateValueChangeListener<State, String?>) {
            stateChange = l
        }

        fun release() {
            queue.clear()
            loopJob?.cancel()
            loopJob = null
            kotlin.runCatching { fos?.close() }
            stateChange = null
        }

        sealed class State

        data object Prepare : State()
        data object Write : State()
        data object Translate : State()
        data object GenerateFile : State()
        data object Complete : State()

        data class Data(val byte: ByteArray, val size: Int = byte.size, val isEnd: Boolean = false) {
            companion object {
                fun end() = Data(byteArrayOf(), 0, true)
            }

            override fun equals(other: Any?) = false
            override fun hashCode() = 0
        }
    }
}

