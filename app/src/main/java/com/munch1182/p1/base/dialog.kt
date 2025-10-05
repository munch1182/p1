package com.munch1182.p1.base

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.viewbinding.ViewBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.munch1182.lib.base.DialogViewCtxProvider
import com.munch1182.lib.helper.AllowDeniedDialog
import com.munch1182.lib.helper.currAsFM
import com.munch1182.lib.helper.result.ResultHelper

object DialogHelper {

    fun newMessage(title: String, msg: String, ok: String = ctx!!.getString(android.R.string.ok), cancel: String = ctx!!.getString(android.R.string.cancel)): AllowDeniedDialog {
        return MessageDialog(ctx!!, title, msg, ok, cancel)
    }

    fun newBottom(isCancel: Boolean = true, content: @Composable (DialogInterface?) -> Unit): DFBottom {
        return DFBottom(isCancel).inject { it, dialog -> ComposeView(it).apply { setContent({ content(dialog) }) } }
    }

    open class DFBottom(private val isCancel: Boolean = true) : BottomSheetDialogFragment(), AllowDeniedDialog {
        private var dProvider: DialogViewCtxProvider? = null
        fun inject(provider: DialogViewCtxProvider?) = this.apply { this.dProvider = provider }

        private var vbInflater: ((LayoutInflater, ViewGroup?, Boolean) -> ViewBinding)? = null
        private var bind: ViewBinding? = null
        private var onViewCreated: ((ViewBinding) -> Unit)? = null

        @Suppress("UNCHECKED_CAST")
        fun <VB : ViewBinding> inject(inflater: (LayoutInflater, ViewGroup?, Boolean) -> VB, onViewCreated: (VB) -> Unit): DFBottom {
            this.vbInflater = inflater
            this.onViewCreated = { onViewCreated.invoke(it as VB) }
            return this
        }

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            val bind = bind ?: vbInflater?.invoke(inflater, container, false)?.apply { bind = this }
            if (bind != null) return bind.root
            return (container?.context ?: context)?.let { dProvider?.onCreateView(it, dialog) } ?: super.onCreateView(inflater, container, savedInstanceState)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            bind?.let { onViewCreated?.invoke(it) }
            dialog?.window?.setBackgroundDrawable(null)
            isCancelable = isCancel
        }

        override val result: Boolean get() = false

        override fun show() {
            show(currAsFM.supportFragmentManager, null)
        }
    }
}

private class MessageDialog(
    ctx: Context, title: String, msg: String,
    ok: String = ctx.getString(android.R.string.ok), cancel: String = ctx.getString(android.R.string.cancel),
    isCancelable: Boolean = true
) : AlertDialog(ctx), AllowDeniedDialog {
    private var _result = false
    override val result get() = _result

    init {
        setTitle(title)
        setMessage(msg)
        setCanceledOnTouchOutside(isCancelable)
        setCancelable(isCancelable)
        setButton(BUTTON_POSITIVE, ok) { _, _ -> _result = true }
        setButton(BUTTON_NEGATIVE, cancel) { _, _ -> _result = false }
    }
}

fun ResultHelper.PermissionsResultHelper.onPermission(vararg req: Pair<String, String>): ResultHelper.PermissionsResultHelper {
    return onDialog { time, _ ->
        when (time) {
            ResultHelper.PermissionDialogTime.BeforeRequest -> null
            ResultHelper.PermissionDialogTime.Denied -> DialogHelper.newMessage("权限请求", req.joinToString(";\n") { "需要授予${it.first}权限用于${it.second}" }, "授权", "拒绝")
            ResultHelper.PermissionDialogTime.NeverAsk -> DialogHelper.newMessage("权限请求", "需要前往设置界面手动允许${req.joinToString("、") { it.first }}权限才能使用该功能", "前往", "拒绝")
        }
    }
}

fun ResultHelper.IntentResultHelper.onIntent(content: String): ResultHelper.IntentResultHelper {
    return onDialog { DialogHelper.newMessage("前往", content, "前往") }
}

fun ResultHelper.JudgeHelper.onIntent(content: String): ResultHelper.JudgeHelper {
    return onDialog { DialogHelper.newMessage("前往", content, "前往") }
}