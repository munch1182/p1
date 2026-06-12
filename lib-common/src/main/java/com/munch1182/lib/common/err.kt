package com.munch1182.lib.common

import kotlinx.coroutines.CoroutineExceptionHandler
import java.io.Closeable
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

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

fun Closeable.closeQuietly() {
    try {
        close()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}