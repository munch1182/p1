package com.munch1182.core.ui.dialog

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.munch1182.core.ui.currAsFragmentActivityOrThrow
import com.munch1182.core.ui.currOrThrow
import com.munch1182.lib.android.IResultPrompt
import com.munch1182.lib.android.IResultPromptControl
import com.munch1182.lib.common.INotice

/**
 * Dialog 工厂，提供统一的弹窗创建方法。
 */
object DialogFactory {

    /**
     * 创建一个 Yes/No 确认弹窗。
     *
     * @param ctx 默认使用当前 Activity，也可显式传入
     */
    fun newYesNoDialog(
        msg: String,
        title: String = "提示",
        ctx: FragmentActivity = currAsFragmentActivityOrThrow,
        cancelable: Boolean = true,
        ok: String = ctx.getString(android.R.string.ok),
        cancel: String = ctx.getString(android.R.string.cancel),
    ): IResultPrompt<Boolean> = CommonDialog(act = ctx, title = title, msg = msg, cancelable = cancelable, ok = ok, cancel = cancel)


    fun newBottom(content: @Composable (IResultPromptControl<Unit>) -> Unit) = newBottom(Unit, content)

    fun <RESULT> newBottom(defVal: RESULT? = null, content: @Composable (IResultPromptControl<RESULT>) -> Unit): IResultPrompt<RESULT> {
        val activity = currAsFragmentActivityOrThrow
        val registry = ViewModelProvider(activity)[ContentRegistryViewModel::class.java] // 绑定在activity中
        // 包装 content：运行时获取 CompositionLocal 中的控制器，再调用用户 content
        val wrappedContent: @Composable () -> Unit = {
            val control = CommonBottomDialogFragment.LocalResultControl.current // 实际提供在CommonBottomDialogFragment的onCreateView
            @Suppress("UNCHECKED_CAST") content(control as IResultPromptControl<RESULT>)
        }
        val key = registry.register(wrappedContent)

        val fragment = CommonBottomDialogFragment.newInstance<RESULT>(key)
        if (defVal != null) registry.setResult(key, defVal)
        fragment.onDismiss { _ -> registry.unregister(key) } // 弹窗关闭时从 registry 中移除
        return fragment
    }
}

/**
 * Toast 通知实现。
 */
object Notice : INotice {

    override fun toast(message: String) {
        Toast.makeText(currOrThrow, message, Toast.LENGTH_SHORT).show()
    }
}
