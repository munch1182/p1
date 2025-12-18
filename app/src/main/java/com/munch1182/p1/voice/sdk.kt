package com.munch1182.p1.voice

import com.iflytek.aikit.core.AiHelper
import com.iflytek.aikit.core.AiResponse
import com.iflytek.aikit.core.BaseLibrary
import com.iflytek.aikit.core.CoreListener
import com.iflytek.aikit.core.ErrType
import com.munch1182.android.lib.AppHelper
import com.munch1182.android.lib.base.Loglog
import com.munch1182.android.lib.base.launchIO
import com.munch1182.p1.base.DataHelper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import java.io.File

class VoiceException(val code: Int, val msg: String? = null) : Exception("error: $code, msg: $msg")

internal fun List<AiResponse?>?.asMap(): Map<String, String>? {
    val list = this?.filterNotNull() ?: return null
    return list.associate { it.key to String(it.value) }
}

fun initSdk(
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

        val file = File(workDir)
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
                .workDir(workDir)
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