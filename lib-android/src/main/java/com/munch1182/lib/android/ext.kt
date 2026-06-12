package com.munch1182.lib.android

/**
 * 对[runCatching]添加异常日志处理
 */
fun <T> Result<T>.logFailure(msg: String = ""): Result<T> = apply {
    onFailure {
        Log.e("logFailure", "catch error: ${msg}: $it")
    }
}