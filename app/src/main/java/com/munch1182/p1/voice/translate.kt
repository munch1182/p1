package com.munch1182.p1.voice

import com.iflytek.aikit.core.AiHelper
import com.iflytek.aikit.core.AiListener
import com.iflytek.aikit.core.AiRequest
import com.iflytek.aikit.core.AiResponse
import com.iflytek.aikit.core.AiText
import com.munch1182.android.lib.base.Logger
import com.munch1182.android.lib.base.Loglog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn

fun Flow<String>.translate(translateType: String, resDir: String) = callbackFlow {
    val abilityId = "ed4f63e83"
    val log = Logger("translate")

    log.logStr("recognize abilityId: $abilityId")
    AiHelper.getInst().registerListener(abilityId, object : AiListener {

        /**
         * （1） pgs：引擎的实时识别结果，属于中间结果，结果为json字符串
         * （2） plain：引擎的最终识别结果，结果为json字符串
         * （3） vad：vad模块对音频做的切分结果，只有vad保留下来的音频片段才会被送入识别引擎，结果为json字符串
         * （4） readable：一般用不上
         * （5） htk：htk格式，方便使用htk工具包去做识别准确率的统计
         */
        override fun onResult(handleId: Int, outputData: List<AiResponse?>?, userCtx: Any?) {
            log.log("onResult: $outputData, $handleId")
            if (outputData != null && outputData.isNotEmpty()) {
                outputData.filterNotNull().forEach {
                    trySend(Result.success(String(it.value)))
                }
            }
        }

        override fun onEvent(handleId: Int, event: Int, eventData: List<AiResponse?>?, userCtx: Any?) {
            log.log("event: $event")
        }

        override fun onError(handleId: Int, err: Int, msg: String?, userCtx: Any?) {
            log.log("onError: $err, $msg")
            trySend(Result.failure(RecognizeException(err, msg)))
        }
    })

    val enginePb = AiRequest.builder().param("res_dir", resDir)
    val initEngine = AiHelper.getInst().engineInit(abilityId, enginePb.build())
    if (initEngine != 0) {
        log.logStr("start Fail: $initEngine")
        trySend(Result.failure(RecognizeException(initEngine, "AiHelper.engineInit Fail: $initEngine")))
        close()
        return@callbackFlow
    }

    collect {
        Loglog.log(111, it)
        val textPb = AiRequest.builder()
        textPb.param("translateType", translateType).param("params", "format=json;data=$it")

        textPb.payload(AiText.get("txt").data(it).valid())
        log.logStr(translateType)

        val write = AiHelper.getInst().oneShotAsync(abilityId, textPb.build())
        if (write != 0) {
            log.logStr("write Fail: $write")
            trySend(Result.failure(RecognizeException(write, "AiHelper.write Fail: $write")))
        }
    }
    awaitClose {
        log.logStr("close translate")
    }
}.flowOn(Dispatchers.IO)