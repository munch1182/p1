package com.munch1182.p1.views

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
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
import com.iflytek.aikit.core.AiAudio
import com.iflytek.aikit.core.AiHelper
import com.iflytek.aikit.core.AiListener
import com.iflytek.aikit.core.AiRequest
import com.iflytek.aikit.core.AiResponse
import com.iflytek.aikit.core.AiStatus
import com.iflytek.aikit.core.BaseLibrary
import com.iflytek.aikit.core.CoreListener
import com.iflytek.aikit.core.ErrType
import com.munch1182.android.lib.AppHelper
import com.munch1182.android.lib.base.Loglog
import com.munch1182.android.lib.base.ReRunJob
import com.munch1182.android.lib.base.appSetting
import com.munch1182.android.lib.base.launchIO
import com.munch1182.android.lib.base.managerAllFiles
import com.munch1182.android.lib.base.selectDir
import com.munch1182.android.lib.helper.RecordHelper
import com.munch1182.android.lib.helper.result.ifAllGranted
import com.munch1182.android.lib.helper.result.ifTrue
import com.munch1182.android.lib.helper.result.intent
import com.munch1182.android.lib.helper.result.isAllGranted
import com.munch1182.android.lib.helper.result.permission
import com.munch1182.p1.base.DataHelper
import com.munch1182.p1.base.onDialog
import com.munch1182.p1.ui.Items
import com.munch1182.p1.ui.SpacerV
import com.munch1182.p1.ui.StateButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.nio.charset.Charset

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
            initSdk(workDir, authFile).collect {
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

    @SuppressLint("MissingPermission")
    private fun startRecord() {

        viewModelScope.launchIO(recordJob.newContext) {
            _uiState.emit(_uiState.value.copy(isRecording = true))
            val record = recordHelper.record()
            val recognized = record.recognize()
            recognized.collect(_result::emit)
        }
    }

    private fun stopRecord() {
        _uiState.value = _uiState.value.copy(isRecording = false)
        recordJob.cancel()
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

fun Flow<ByteArray>.recognize(langType: Int = 0) = callbackFlow {
    val abilityId = "e0e26945b"

    Loglog.log("abilityId: $abilityId")
    AiHelper.getInst().registerListener(abilityId, object : AiListener {
        /**
         * 与varType无关，都是String
         */
        private fun AiResponse.getValueCompat(): String {
            val value = value.copyOf(len)
            val string = String(value, Charset.forName(if (langType == 0) "GBK" else "UTF-8"))
            Loglog.log(key, type, varType, string)
            return string
        }

        override fun onResult(handleId: Int, outputData: List<AiResponse?>?, userCtx: Any?) {
            if (outputData != null && outputData.isNotEmpty()) {
                val result = HashMap<String, String>(outputData.size)
                outputData.forEach { i ->
                    i ?: return@forEach
                    val key = i.key ?: return@forEach
                    val value = i.getValueCompat()
                    result[key] = value
                }
                val str = result.get("pgs")
                Loglog.log(str)
                str?.let { trySend(str) }
            }
        }

        override fun onEvent(handleId: Int, event: Int, eventData: List<AiResponse?>?, userCtx: Any?) {
            Loglog.log("event: $event")
        }

        override fun onError(handleId: Int, err: Int, msg: String?, userCtx: Any?) {
            Loglog.log("onError: $err, $msg")
        }
    })

    val paramBuilder = AiRequest.Builder()

    paramBuilder.param("languageType", langType)
    paramBuilder.param("vadOn", true)
    paramBuilder.param("rltSep", "blank")
    paramBuilder.param("vadEnergyThreshold", 9)
    paramBuilder.param("vadThreshold", 0.1332)
    paramBuilder.param("vadSpeechEnd", 180)
    paramBuilder.param("vadResponsetime", 1000)
    paramBuilder.param("vadLinkOn", false)
    paramBuilder.param("pureEnglish", false)
    paramBuilder.param("outputType", 0)
    paramBuilder.param("puncCache", true)
    paramBuilder.param("postprocOn", true)
    paramBuilder.param("vadEndGap", 40)

    val handle = AiHelper.getInst().start(abilityId, paramBuilder.build(), 1)
    if (!handle.isSuccess) {
        Loglog.log("start Fail: ${handle.code}")
        close()
        return@callbackFlow
    }

    var isBegin = false

    collect {
        val builder = AiRequest.Builder()
        val audioData = AiAudio.get("input").encoding(AiAudio.ENCODING_PCM).data(it)
        audioData.status(if (isBegin) AiStatus.CONTINUE else AiStatus.BEGIN)
        isBegin = true
        builder.payload(audioData.valid())
        val restWrite = AiHelper.getInst().write(builder.build(), handle)
        if (restWrite != 0) {
            Loglog.log("restWrite: $restWrite")
            close()
        }
        val restRead = AiHelper.getInst().read(abilityId, handle)
        if (restRead != 0) {
            Loglog.log("restRead: $restRead")
            close()
        }
    }
    awaitClose {
        Loglog.log("close recognize")
        AiHelper.getInst().end(handle)
    }
}.flowOn(Dispatchers.IO)

private fun initSdk(
    workDir: String, authFile: String? = null, authType: BaseLibrary.AuthType = BaseLibrary.AuthType.DEVICE
) = callbackFlow {

    launchIO {
        val data = DataHelper.Config.Translate.get()
        if (data == null) {
            send(InitState.Failed("无配置文件"))
            close()
            return@launchIO
        }
        AiHelper.getInst().registerListener(object : CoreListener {
            override fun onAuthStateChange(p0: ErrType?, p1: Int) {
                Loglog.log("auth $p0, ${p1 == 0}, $p1")
                when (p0) {
                    ErrType.AUTH -> {}
                    ErrType.HTTP -> {}
                    null, ErrType.UNKNOWN -> false
                }
                trySend(if (p1 == 0) InitState.Success else InitState.Failed("auth failed: $p1"))
            }
        })
        val workDir2 = try {
            uir2FilePart(workDir.toUri().toString())?.absolutePath ?: workDir
        } catch (e: Exception) {
            e.printStackTrace()
            send(InitState.Failed("文件不支持：$workDir"))
            close()
            return@launchIO
        }
        val file = File(workDir2)
        if (!file.canRead() || !file.canWrite()) {
            send(InitState.Failed("文件没有读写权限：$workDir"))
            close()
            return@launchIO
        }
        AiHelper.getInst().initEntry(
            AppHelper, BaseLibrary.Params.Builder()
                .appId(data.appId)
                .apiSecret(data.appSecret)
                .apiKey(data.apiKey)
                .workDir(workDir2)
                .authInterval(30 * 25 * 60 * 60)
                .authType(authType).apply {
                    authFile?.let { file -> licenseFile(file) }
                }.build()
        )
    }
    awaitClose {
        Loglog.log("close sdk")
        AiHelper.getInst().unInit()
    }
}

sealed class InitState {
    object Success : InitState()
    data class Failed(val e: Exception) : InitState() {

        constructor(str: String) : this(IllegalStateException(str))
    }
}

/**
 * 仅支持部分转换，不能作为最终方法使用
 */
private fun uir2FilePart(str: String): File? {
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