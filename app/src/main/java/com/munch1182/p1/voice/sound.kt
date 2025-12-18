package com.munch1182.p1.voice

import androidx.annotation.IntRange
import com.iflytek.aikit.core.AiHelper
import com.iflytek.aikit.core.AiListener
import com.iflytek.aikit.core.AiRequest
import com.iflytek.aikit.core.AiResponse
import com.iflytek.aikit.core.AiText
import com.munch1182.android.lib.base.Logger
import com.munch1182.android.lib.base.Loglog
import com.munch1182.android.lib.base.splitArray
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * "catherine","keshu","mohita","miya","thuhien","zhongcun","xiaoyi","lisa","aurora","christiane","rania","suparut"
 *
 * catherine:英文青年女声交互, keshu:俄语女声,  mohita:印地语女声, miya:韩语女声,  thuhien:越南语女声, zhongcun:日语女声,  xiaoyi:中文青年女声客服,
 * lisa:法语女声, aurora:西语女声, christiane:德语女声, rania:阿拉伯语女声,suparut:泰语女声,
 *
 */
private fun getVcnByLang(language: Int): String {
    return when (language) {
        1 -> "xiaoyi"
        2 -> "catherine"
        6 -> "keshu"
        8 -> "mohita"
        16 -> "miya"
        11 -> "thuhien"
        5 -> "zhongcun"
        97 -> "lisa"
        23 -> "aurora"
        93 -> "christiane"
        88 -> "rania"
        27 -> "suparut"
        else -> throw UnsupportedOperationException("language: $language not support")
    }
}

/**

 *
 * @param language
 *
 * 1:中文, 2:英文, 6:俄语, 10:藏语, 8:印地语, 16:韩语, 99:维语, 11:越南语, 5:日语, 66:缅甸语,
 * 93:德语, 97:法语, 23:西语, 27:泰语, 88:阿拉伯语, 12:香港粤语, 102:美式英文,
 * 38:闽南语, 13:巴西葡萄牙语, 22:欧洲葡萄牙语
 */
fun Flow<String>.speak(
    language: Int, encode: String = "UTF-8",
    @IntRange(from = 0, to = 100) speed: Int = 80,
    @IntRange(from = 0, to = 100) volume: Int = 100
) = callbackFlow {
    val abilityId = "e09712bcb"
    val log = Logger("sound")
    val vcn = getVcnByLang(language)
    log.logStr("sound abilityId: $abilityId, vcn: $vcn, language:$language, speed:$speed, volume:$volume")

    AiHelper.getInst().registerListener(abilityId, object : AiListener {
        override fun onResult(p0: Int, p1: List<AiResponse?>?, p2: Any?) {
            p1?.forEach { p ->
                log.logStr("onResult: ${p?.key}")
                val bytes = p?.value ?: return@forEach
                bytes.splitArray(50).forEach {
                    Loglog.log(it.toHexString())
                }
                trySend(Result.success(bytes))
            }
            log.logStr("onResult: --------")
        }

        override fun onEvent(handleId: Int, event: Int, eventData: List<AiResponse?>?, userCtx: Any?) {
            log.logStr("event: $event")
        }

        override fun onError(handleId: Int, err: Int, msg: String?, userCtx: Any?) {
            log.logStr("onError: $err, $msg")
            trySend(Result.failure(VoiceException(err, msg)))
        }
    })

    val startPM = AiRequest.Builder().param("vcn", vcn).param("language", language).param("speed", speed).param("volume", volume).param("pitch", 50).param("textEncoding", encode).build()
    val handle = AiHelper.getInst().start(abilityId, startPM, null)
    if (!handle.isSuccess) {
        log.logStr("start failed: ${handle.code}")
        trySend(Result.failure(VoiceException(handle.code, "AiHelper.start Fail: ${handle.code}")))
        close()
        return@callbackFlow
    }
    collect {
        log.logStr("sound: $it")
        val writeBuilder = AiRequest.builder()
        val text = try {
            AiText.get("text").data(it).valid()
        } catch (e: Exception) {
            log.logStr("sound: text valid fail: ${e.message}")
            return@collect
        }
        writeBuilder.payload(text)
        val write = AiHelper.getInst().write(writeBuilder.build(), handle)
        if (write != 0) {
            log.logStr("start failed: $write")
            trySend(Result.failure(VoiceException(write, "AiHelper.start Fail: $write")))
            close()
        }
        delay(100L)
    }

    awaitClose {
        AiHelper.getInst().end(handle)
        log.logStr("close voice")
    }
}