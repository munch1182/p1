package com.munch1182.p1.voice

import com.iflytek.aikit.core.AiAudio
import com.iflytek.aikit.core.AiHelper
import com.iflytek.aikit.core.AiListener
import com.iflytek.aikit.core.AiRequest
import com.iflytek.aikit.core.AiResponse
import com.iflytek.aikit.core.AiStatus
import com.munch1182.android.lib.base.Logger
import com.munch1182.android.net.gson
import com.munch1182.android.net.jsonCatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import java.nio.charset.Charset

sealed class RecognizedState {
    object Recognizing : RecognizedState()
    object Recognized : RecognizedState()
}

private data class Plain(val ws: List<PlainSc>)

private data class PlainSc(val w: String)

class RecognizeException(val code: Int, val msg: String? = null) : Exception("error: $code, msg: $msg")

data class Recognized(val str: String, val state: RecognizedState)

fun Flow<ByteArray>.recognize(langType: Int = 0) = callbackFlow {
    val abilityId = "e0e26945b"
    val log = Logger("recognize")

    log.logStr("recognize abilityId: $abilityId")
    AiHelper.getInst().registerListener(abilityId, object : AiListener {
        /**
         * 与varType无关，都是String
         */
        private fun AiResponse.getValueCompat(): String {
            val value = value.copyOf(len)
            return String(value, Charset.forName(if (langType == 0) "GBK" else "UTF-8"))
        }

        private fun fromStr(str: String?): String? {
            str ?: return null
            val plain = gson.jsonCatch<Plain>(str) ?: return null
            return plain.ws.joinToString("") { it.w }
        }

        /**
         * （1） pgs：引擎的实时识别结果，属于中间结果，结果为json字符串
         * （2） plain：引擎的最终识别结果，结果为json字符串
         * （3） vad：vad模块对音频做的切分结果，只有vad保留下来的音频片段才会被送入识别引擎，结果为json字符串
         * （4） readable：一般用不上
         * （5） htk：htk格式，方便使用htk工具包去做识别准确率的统计
         */
        override fun onResult(handleId: Int, outputData: List<AiResponse?>?, userCtx: Any?) {
            if (outputData != null && outputData.isNotEmpty()) {
                val result = HashMap<String, String>(outputData.size)
                outputData.forEach { i ->
                    i ?: return@forEach
                    val key = i.key ?: return@forEach
                    val value = i.getValueCompat()
                    result[key] = value
                }
                val pgs = result.get("pgs")
                log.logStr("pgs: $pgs")
                if (!pgs.isNullOrEmpty()) {
                    trySend(Result.success(Recognized(pgs, RecognizedState.Recognizing)))
                }
                val plain = result.get("plain")
                log.logStr("plain: $plain")
                val str = fromStr(plain)
                if (!str.isNullOrEmpty()) {
                    trySend(Result.success(Recognized(str, RecognizedState.Recognized)))
                }
            }
        }

        override fun onEvent(handleId: Int, event: Int, eventData: List<AiResponse?>?, userCtx: Any?) {
            log.logStr("event: $event")
        }

        override fun onError(handleId: Int, err: Int, msg: String?, userCtx: Any?) {
            log.logStr("onError: $err, $msg")
            trySend(Result.failure(RecognizeException(err, msg)))
        }
    })

    // https://www.xfyun.cn/doc/asr/AIkit_offline_iat/Android-SDK.html#_6-%E5%BF%AB%E9%80%9F%E9%9B%86%E6%88%90%E6%8C%87%E5%8D%97
    val pb = AiRequest.Builder()
    pb.param("languageType", langType)
    pb.param("vadOn", true)
    pb.param("rltSep", "blank")
    pb.param("vadEnergyThreshold", 9)
    pb.param("vadThreshold", 0.1332)
    pb.param("vadSpeechEnd", 150000) // vad强行结束时间, 最小值:0, 最大值:150000，默认值180
    pb.param("vadResponsetime", 150000) // vad前端点超时时间，单位：10ms, 最小值:0, 最大值:170000, 默认值1000
    pb.param("vadLinkOn", false)
    pb.param("pureEnglish", false)
    pb.param("outputType", 0)
    pb.param("puncCache", false) // 句尾标点是否为缓存模式
    pb.param("postprocOn", true)
    pb.param("vadEndGap", 40)

    val handle = AiHelper.getInst().start(abilityId, pb.build(), 1)
    if (!handle.isSuccess) {
        log.logStr("start Fail: ${handle.code}")
        trySend(Result.failure(RecognizeException(handle.code, "AiHelper.start Fail: ${handle.code}")))
        close()
        return@callbackFlow
    }

    var isBegin = false

    collect {
        val newBuilder = AiRequest.Builder()
        val audioData = AiAudio.get("input").encoding(AiAudio.ENCODING_PCM).data(it)
        val state = if (isBegin) AiStatus.CONTINUE else AiStatus.BEGIN
        audioData.status(state)
        isBegin = true
        newBuilder.payload(audioData.valid())
        val restWrite = AiHelper.getInst().write(newBuilder.build(), handle)
        if (restWrite != 0) {
            log.logStr("restWrite: $restWrite")
            trySend(Result.failure(RecognizeException(restWrite, "AiHelper.write Fail: $restWrite")))
            close()
        }
        val restRead = AiHelper.getInst().read(abilityId, handle)
        if (restRead != 0) {
            log.logStr("restRead: $restWrite")
            trySend(Result.failure(RecognizeException(restRead, "AiHelper.read Fail: $restRead")))
            close()
        }
    }
    awaitClose {
        log.logStr("close recognize")
        AiHelper.getInst().end(handle)
    }
}.flowOn(Dispatchers.IO)

