package com.munch1182.p1.base

import com.munch1182.core.android.Log
import com.munch1182.p1.log.TAG_CATCH_ERROR
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn


/**
 * 对[runCatching]添加异常日志处理
 */
fun <T> Result<T>.logFailure(msg: String = ""): Result<T> = apply {
    onFailure {
        Log.e(TAG_CATCH_ERROR, "catch error: ${msg}: $it")
    }
}

fun <T> Flow<T>.stateIn(
    scope: CoroutineScope, initValue: T,
    started: SharingStarted = SharingStarted.WhileSubscribed(5000)
) = stateIn(
    scope, started, initValue
)