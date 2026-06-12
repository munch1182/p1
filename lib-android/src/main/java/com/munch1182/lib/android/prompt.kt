package com.munch1182.lib.android

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * 提示组件的基础定义（可显示/隐藏）
 * 适用于 Dialog、Snackbar、BottomSheet 等任意可展示并关闭的 UI 组件
 */
interface IPrompt {
    fun show()
    fun dismiss()
}

/**
 * 带有状态监听及结果返回的提示组件
 * 支持在显示/关闭时注册回调，并可通过 [awaitResult] 挂起等待关闭时返回的结果
 */
interface IResultPrompt<T> : IPrompt {
    fun onShow(onShow: () -> Unit): IResultPrompt<T>
    fun onDismiss(onDismiss: (T?) -> Unit): IResultPrompt<T>
}

interface IResultPromptControl<T> {
    /** 当前结果（可能为 null，取决于是否已设置或存在默认值） */
    val current: T?

    /** 设置结果（不自动关闭弹窗） */
    fun set(value: T)

    /** 关闭弹窗（不会自动设置结果，结果以最后一次 set 或默认值为准） */
    fun dismiss()

    /** 控制弹窗是否可取消（点击外部/返回键） */
    fun setCancellable(cancellable: Boolean)

    /** 便捷方法：设置结果并关闭弹窗 */
    fun dismissWith(value: T) {
        set(value)
        dismiss()
    }
}

/**
 * 挂起等待 [IResultPrompt] 关闭，并返回 [IResultPrompt.onDismiss] 中传递的结果
 */
suspend fun <T> IResultPrompt<T>.awaitResult(): T? = withUi {
    suspendCancellableCoroutine { coroutine ->
        onDismiss { result ->
            if (coroutine.isActive) coroutine.resume(result)
        }.show()
    }
}