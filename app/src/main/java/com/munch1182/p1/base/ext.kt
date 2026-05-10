package com.munch1182.p1.base

import com.munch1182.core.android.Log
import com.munch1182.p1.log.TAG_CATCH_ERROR


/**
 * 对[runCatching]添加异常日志处理
 */
fun <T> Result<T>.logFailure(msg: String = ""): Result<T> = apply {
    onFailure {
        Log.e(TAG_CATCH_ERROR, "catch error: ${msg}: $it")
    }
}