package com.munch1182.p1.voice

import com.iflytek.aikit.core.AiHelper
import com.iflytek.aikit.core.AiRequest
import com.iflytek.aikit.core.AiText
import com.munch1182.android.lib.base.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn

/**
 * @param resDir 只支持文件路径，不支持uri路径
 */
fun Flow<String>.translate(translateType: String, resDir: String) = callbackFlow {
    val abilityId = "ed4f63e83"
    val log = Logger("translate")
    log.logStr("recognize abilityId: $abilityId, res_dir: $resDir")

    val enginePb = AiRequest.builder().param("res_dir", resDir)
    val initEngine = AiHelper.getInst().engineInit(abilityId, enginePb.build())
    if (initEngine != 0) {
        log.logStr("engineInit Fail: $initEngine")
        trySend(Result.failure(VoiceException(initEngine, "AiHelper.engineInit Fail: $initEngine")))
        close()
        return@callbackFlow
    }

    collect {
        val dataBuilder = AiRequest.builder()
        dataBuilder.param("translateType", translateType).param("params", "type=${translateType}")
        val text = try {
            AiText.get("txt").data(it).valid()
        } catch (e: Exception) {
            log.logStr("translate: text valid fail: ${e.message}")
            return@collect
        }
        dataBuilder.payload(text)

        val trans = AiHelper.getInst().oneShotSync(abilityId, dataBuilder.build(), null)
        if (trans.code == 0) {
            val str = trans.data?.firstOrNull()?.value?.let { b -> String(b) }
            log.logStr("translate: $it == $translateType ==> $str (${trans.data?.size ?: 0})")
            trySend(Result.success(str))
        } else {
            trySend(Result.failure(VoiceException(trans.code, "AiHelper.engineInit Fail: ${trans.code}")))
        }
    }
    awaitClose {
        log.logStr("close translate")
    }
}.flowOn(Dispatchers.IO)