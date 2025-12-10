package com.munch1182.lib.helper

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

interface IDialog : LifecycleOwner {
    fun show()
    fun dismiss()
}

interface ResultDialog<Result> : IDialog {
    val result: Result
}

interface AllowDeniedDialog : ResultDialog<Boolean>

/**
 * 当[ResultDialog]销毁时，回调其[ResultDialog.result]
 *
 * 相当于给所有[ResultDialog]的类增加了回调
 */
fun <R> ResultDialog<R>.onResult(on: (R) -> Unit): ResultDialog<R> {
    lifecycle.addObserver(object : DefaultLifecycleObserver {
        override fun onDestroy(owner: LifecycleOwner) {
            on(result)
        }
    })
    return this
}

suspend fun AllowDeniedDialog.isAllow() = suspendCancellableCoroutine { c -> onResult { c.resume(it) }.show() }