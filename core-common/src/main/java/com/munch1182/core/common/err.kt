package com.munch1182.core.common

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/**
 * 拦截崩溃, 并返回null; 只适用于不关心异常的情形
 */
inline fun <reified T> err2Null(any: () -> T): T? {
    return try {
        any()
    } catch (_: Exception) {
        null
    }
}

/**
 * 简单实现的 [CoroutineExceptionHandler]
 */
class DefaultCoroutineExceptionHandler(private val log: Logger) : //
    AbstractCoroutineContextElement(CoroutineExceptionHandler), //
    CoroutineExceptionHandler { //
    override fun handleException(context: CoroutineContext, exception: Throwable) {
        log.e(
            "DefaultCoroutineExceptionHandler", "CoroutineException: context: $context: exception: $exception"
        )
    }
}