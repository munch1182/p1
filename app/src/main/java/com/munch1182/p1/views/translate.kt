package com.munch1182.p1.views

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.media.AudioFormat
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.munch1182.android.lib.AppHelper
import com.munch1182.android.lib.base.Loglog
import com.munch1182.android.lib.base.ReRunJob
import com.munch1182.android.lib.base.appSetting
import com.munch1182.android.lib.base.launchIO
import com.munch1182.android.lib.base.managerAllFiles
import com.munch1182.android.lib.base.selectDir
import com.munch1182.android.lib.base.splitArray
import com.munch1182.android.lib.helper.AudioStreamHelper
import com.munch1182.android.lib.helper.FileHelper
import com.munch1182.android.lib.helper.FileWriteHelper
import com.munch1182.android.lib.helper.RecordHelper
import com.munch1182.android.lib.helper.result.ifAllGranted
import com.munch1182.android.lib.helper.result.ifTrue
import com.munch1182.android.lib.helper.result.intent
import com.munch1182.android.lib.helper.result.isAllGranted
import com.munch1182.android.lib.helper.result.permission
import com.munch1182.p1.base.DataHelper
import com.munch1182.p1.base.onDialog
import com.munch1182.p1.ui.ClickButton
import com.munch1182.p1.ui.Items
import com.munch1182.p1.ui.SpacerV
import com.munch1182.p1.ui.StateButton
import com.munch1182.p1.voice.InitState
import com.munch1182.p1.voice.VoiceException
import com.munch1182.p1.voice.initSdk
import com.munch1182.p1.voice.speak
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import java.io.File

@SuppressLint("MissingPermission")
@Composable
fun TranslateView(vm: TranslateVM = viewModel()) {

    val uiState by vm.uiState.collectAsState()

    Items(Modifier.fillMaxWidth()) {
        StateButton(if (uiState.isRecording) "Stop" else "Start", uiState.isRecording) {
            if (uiState.workDir.isEmpty()) {
                intent(selectDir().apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                }).onDialog("请选择资源文件夹所在路径作为工作目录").request {
                    val uri = it?.data?.data
                    uri?.let { uri ->
                        AppHelper.contentResolver.takePersistableUriPermission(
                            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                    }
                    vm.setFile(workDir = uri?.toString())
                }
                return@StateButton
            }
            if (!uiState.isAuthed) {
                withPermission { vm.requestAuth() }
            } else {
                withPermission { vm.toggleRecord() }
            }
        }

        if (uiState.isRecording) {
            ClickButton("发送") { vm.send() }
        }

        SpacerV()

        Text(uiState.state)
        Result(vm)
    }
}

@Composable
private fun Result(vm: TranslateVM) {
    val str by vm.recognized.collectAsState()
    Text(str)
}

@Stable
class TranslateVM : ViewModel() {
    private val _uiState = MutableStateFlow(TranslateUiState())
    private val _result = MutableStateFlow("")
    val uiState = _uiState.asStateFlow()
    val recognized = _result.asStateFlow()
    private val authJob = ReRunJob()
    private val recordJob = ReRunJob()
    private val recordHelper by lazy { RecordHelper.recognition() }

    init {
        viewModelScope.launchIO {
            var workDir = DataHelper.TranslateAuth.WorkDir.get<String?>()
            val uri = workDir?.toUri()
            if (uri != null) {
                val isPermission = AppHelper.contentResolver.persistedUriPermissions.any { it.uri == uri && it.isWritePermission && it.isReadPermission }
                workDir = if (isPermission) workDir else null
            }
            _uiState.value = _uiState.value.copy(
                DataHelper.TranslateAuth.Auth.get() ?: "", workDir ?: ""
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        authJob.cancel()
        recordHelper.release()
        recordJob.cancel()
    }

    fun setFile(auth: String? = null, workDir: String? = null) {
        viewModelScope.launchIO {
            auth?.let { DataHelper.TranslateAuth.Auth.save(it) }
            workDir?.let { DataHelper.TranslateAuth.WorkDir.save(it) }
            _uiState.value = _uiState.value.copy(
                authFile = auth ?: _uiState.value.authFile, workDir = workDir ?: _uiState.value.workDir
            )
        }
    }

    fun requestAuth() {
        viewModelScope.launchIO(authJob.newContext) {
            val authFile = uiState.value.authFile
            val workDir = uiState.value.workDir
            Loglog.log("requestAuth: $authFile, $workDir")
            if (workDir.isEmpty()) return@launchIO

            initSdk(workDir.toTheFile(), authFile).collect {
                when (it) {
                    is InitState.Failed -> {
                        _uiState.value = _uiState.value.copy(isAuthed = false, state = it.e.message ?: "授权失败")
                        authJob.cancel()
                    }

                    InitState.Success -> {
                        _uiState.value = _uiState.value.copy(isAuthed = true, state = "授权成功")
                    }
                }
            }
        }
    }

    fun toggleRecord() {
        if (uiState.value.isRecording) stopRecord() else startRecord()
    }

    private fun String.toTheFile(): String {
        return try {
            uir2FilePart(toUri().toString())?.absolutePath ?: this
        } catch (e: Exception) {
            e.printStackTrace()
            this
        }
    }

    private val record = Channel<String>()

    @SuppressLint("MissingPermission")
    private fun startRecord() {
        val player = AudioStreamHelper(
            24000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        player.prepare()
        viewModelScope.launchIO(recordJob.newContext) {
            _uiState.emit(_uiState.value.copy(isRecording = true))/*val record = recordHelper.record()
            val recognized = record.recognize()
            val trans = recognized.filter { it.isSuccess }
                .map { it.getOrNull()?.str ?: "" }
                .translate("cnen", workDir.toTheFile())*/

            val trans = record.receiveAsFlow().speak(2)
            val write = FileWriteHelper()

            var a = 0
            write.prepare(FileHelper.newFile("aaa", "aaa.pcm"))
            trans.collect {
                when {
                    it.isSuccess -> {
                        a += 1
                        //_result.value = it.getOrNull() ?: ""
                        it.getOrNull()?.let { b ->
                            write.write(b)
                            b.splitArray(player.bufferSize).forEach { d ->
                                player.write(d)
                            }
                        }

                    }

                    it.isFailure -> {
                        val exception = it.exceptionOrNull()
                        if (exception is VoiceException) {
                            _result.value = exception.message ?: "error"
                        } else {
                            _result.value = exception?.stackTraceToString() ?: "error"
                        }
                    }
                }
            }
        }
    }

    private fun stopRecord() {
        _uiState.value = _uiState.value.copy(isRecording = false)
        recordJob.cancel()
    }

    fun send() {
        viewModelScope.launchIO {
            record.send("In boundless desert lonely smokes rise straight; Over endless river the sun sinks round")
        }
    }
}

@Stable
data class TranslateUiState(
    val authFile: String = "", val workDir: String = "", val isAuthed: Boolean = false, val state: String = "", val isRecording: Boolean = false
)

private fun withPermission(any: () -> Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        permission(
            Manifest.permission.READ_PHONE_STATE, Manifest.permission.RECORD_AUDIO
        ).onDialog("翻译相关权限", "翻译").onIntent(appSetting()).ifAllGranted().judge(
            { Environment.isExternalStorageManager() }, managerAllFiles()
        ).onDialog("文件权限", "请授权管理全部文件的权限").ifTrue().request {
            if (it) any()
        }

    } else {
        permission(
            Manifest.permission.READ_PHONE_STATE, Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_EXTERNAL_STORAGE
        ).onDialog("翻译相关权限", "翻译").onIntent(appSetting()).request {
            if (it.isAllGranted()) any()
        }
    }
}

/**
 * 仅支持部分转换，不能作为最终方法使用
 */
fun uir2FilePart(str: String): File? {
    val uri = str.toUri()
    if (DocumentsContract.isTreeUri(uri)) {
        val docId = DocumentsContract.getTreeDocumentId(uri)
        return if (docId.startsWith("primary:")) {
            val path = docId.split(":")[1]
            File("${Environment.getExternalStorageDirectory().path}/$path")
        } else {
            null
        }
    }
    return null
}