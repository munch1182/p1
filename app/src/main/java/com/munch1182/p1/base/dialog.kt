package com.munch1182.p1.base

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.viewbinding.ViewBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.munch1182.lib.helper.AllowDeniedDialog
import com.munch1182.lib.helper.ResultDialog
import com.munch1182.lib.helper.currAsFM
import com.munch1182.lib.helper.result.ResultChainHelper
import com.munch1182.lib.helper.result.ResultHelper

object DialogHelper {

    fun newMessage(title: String, msg: String, ok: String = ctx!!.getString(android.R.string.ok), cancel: String = ctx!!.getString(android.R.string.cancel)): AllowDeniedDialog {
        return MessageDialog(ctx!!, title, msg, ok, cancel)
    }

    fun newBottom(isCancel: Boolean = true, content: @Composable () -> Unit) = newBottom(false, isCancel) { content() }

    fun <RESULT> newBottom(result: RESULT, isCancel: Boolean = true, content: @Composable (SimpleData<RESULT>) -> Unit): DFBottom<RESULT> {
        return DFBottom(result, isCancel).inject { ctx, result -> ComposeView(ctx).apply { setContent { content(result) } } }
    }

    @FunctionalInterface
    fun interface ResultDialogViewProvide<RESULT> {
        fun onCreateView(ctx: Context, result: SimpleData<RESULT>): View?
    }

    class SimpleData<T>(private var _data: T) {
        fun update(newData: T) {
            _data = newData
        }

        val data get() = _data
    }

    open class DFBottom<RESULT>(result: RESULT, private val isCancel: Boolean = true) : BottomSheetDialogFragment(), ResultDialog<RESULT> {
        private val _result = SimpleData(result)
        private var dProvider: ResultDialogViewProvide<RESULT>? = null
        fun inject(provider: ResultDialogViewProvide<RESULT>?) = this.apply { this.dProvider = provider }

        private var vbInflater: ((LayoutInflater, ViewGroup?, Boolean) -> ViewBinding)? = null
        private var bind: ViewBinding? = null
        private var onViewCreated: ((ViewBinding) -> Unit)? = null

        @Suppress("UNCHECKED_CAST")
        fun <VB : ViewBinding> inject(inflater: (LayoutInflater, ViewGroup?, Boolean) -> VB, onViewCreated: (VB) -> Unit): DFBottom<RESULT> {
            this.vbInflater = inflater
            this.onViewCreated = { onViewCreated.invoke(it as VB) }
            return this
        }

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            val bind = bind ?: vbInflater?.invoke(inflater, container, false)?.apply { bind = this }
            if (bind != null) return bind.root
            return (container?.context ?: context)?.let { dProvider?.onCreateView(it, _result) } ?: super.onCreateView(inflater, container, savedInstanceState)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            bind?.let { onViewCreated?.invoke(it) }
            dialog?.window?.setBackgroundDrawable(null)
            isCancelable = isCancel
        }

        override val result: RESULT get() = _result.data

        override fun show() {
            show(currAsFM.supportFragmentManager, null)
        }
    }
}

private class MessageDialog(
    ctx: Context, title: String, msg: String, ok: String = ctx.getString(android.R.string.ok), cancel: String = ctx.getString(android.R.string.cancel), isCancelable: Boolean = true
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

private fun onPermissionDialog(time: ResultHelper.PermissionDialogTime, vararg req: Pair<String, String>) = when (time) {
    ResultHelper.PermissionDialogTime.BeforeRequest -> null
    ResultHelper.PermissionDialogTime.Denied -> DialogHelper.newMessage("权限请求", req.joinToString(";\n") { "需要授予${it.first}权限用于${it.second}" }, "授权", "拒绝")
    ResultHelper.PermissionDialogTime.NeverAsk -> DialogHelper.newMessage("权限请求", "需要前往设置界面手动允许${req.joinToString("、") { it.first }}权限才能使用该功能", "前往", "拒绝")
}

fun ResultHelper.PermissionsResultHelper.onPermission(vararg req: Pair<String, String>) = onDialog { time, _ -> onPermissionDialog(time, *req) }

fun ResultChainHelper.ChainPermissionsResultHelper.onPermission(vararg req: Pair<String, String>) = onDialog { time, _ -> onPermissionDialog(time, *req) }

private fun onIntentDialog(content: String) = DialogHelper.newMessage("前往", content, "前往")

fun ResultHelper.IntentResultHelper.onIntent(content: String) = onDialog { onIntentDialog(content) }

fun ResultHelper.JudgeResultHelper.onIntent(content: String) = onDialog { onIntentDialog(content) }

fun ResultChainHelper.ChainIntentResultHelper.onIntent(content: String) = onDialog { onIntentDialog(content) }
fun ResultChainHelper.ChainJudgeResultHelper.onIntent(content: String) = onDialog { onIntentDialog(content) }