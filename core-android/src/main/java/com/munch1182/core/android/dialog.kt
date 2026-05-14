package com.munch1182.core.android

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

interface IDialog {
    fun show()
    fun dismiss()
}

interface IResultDialog<T> : IDialog {
    fun onShow(onShow: () -> Unit): IResultDialog<T>
    fun onDismiss(onDismiss: (T) -> Unit): IResultDialog<T>
}

/**
 * 获取[IResultDialog.onDismiss]中的结果
 */
suspend fun <T> IResultDialog<T>.awaitResult(): T = withUi {
    suspendCancellableCoroutine { coroutine ->
        onDismiss { result ->
            if (coroutine.isActive) coroutine.resume(result)
        }.show()
    }
}