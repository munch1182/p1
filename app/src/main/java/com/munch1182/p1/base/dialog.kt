package com.munch1182.p1.base

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.viewbinding.ViewBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.munch1182.android.lib.helper.AllowDeniedDialog
import com.munch1182.android.lib.helper.ResultDialog
import com.munch1182.android.lib.helper.currAsFM
import com.munch1182.android.lib.helper.result.IPermissionWithDialog
import com.munch1182.p1.ui.createComposeView

object DialogHelper {

    fun newMessage(title: String, msg: String, ok: String = ctx!!.getString(android.R.string.ok), cancel: String = ctx!!.getString(android.R.string.cancel)): AllowDeniedDialog {
        return MessageDialog(ctx!!, title, msg, ok, cancel)
    }

    fun newBottom(isCancel: Boolean = true, content: @Composable (BottomSheetDialogFragment) -> Unit) = newBottom(false, isCancel) { _, fg -> content(fg) }

    fun <VB : ViewBinding> newBottom(inflater: (LayoutInflater, ViewGroup?, Boolean) -> VB, onViewCreated: (VB, BottomSheetDialogFragment) -> Unit): DFBottom<Boolean> {
        return DFBottom(false, isCancel = true).inject(inflater, onViewCreated)
    }

    fun <RESULT> newBottom(result: RESULT, isCancel: Boolean = true, content: @Composable (SimpleData<RESULT>) -> Unit): DFBottom<RESULT> {
        return DFBottom(result, isCancel).inject { ctx, result, _ -> createComposeView(ctx) { content(result) } }
    }

    fun <RESULT> newBottom(result: RESULT, isCancel: Boolean = true, content: @Composable (SimpleData<RESULT>, BottomSheetDialogFragment) -> Unit): DFBottom<RESULT> {
        return DFBottom(result, isCancel).inject { ctx, result, fg -> createComposeView(ctx) { content(result, fg) } }
    }

    fun newTopNotice(content: @Composable () -> Unit): IPermissionWithDialog = TopNoticeDialog(createComposeView(currAsFM, content))
}

@FunctionalInterface
fun interface ResultDialogViewProvide<RESULT> {
    fun onCreateView(ctx: Context, result: SimpleData<RESULT>, frag: BottomSheetDialogFragment): View?
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
    private var onViewCreated: ((ViewBinding, BottomSheetDialogFragment) -> Unit)? = null

    @Suppress("UNCHECKED_CAST")
    fun <VB : ViewBinding> inject(inflater: (LayoutInflater, ViewGroup?, Boolean) -> VB, onViewCreated: (VB, BottomSheetDialogFragment) -> Unit): DFBottom<RESULT> {
        this.vbInflater = inflater
        this.onViewCreated = { it, fg -> onViewCreated.invoke(it as VB, fg) }
        return this
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val bind = bind ?: vbInflater?.invoke(inflater, container, false)?.apply { bind = this }
        if (bind != null) return bind.root
        return (container?.context ?: context)?.let { dProvider?.onCreateView(it, _result, this) } ?: super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bind?.let { onViewCreated?.invoke(it, this@DFBottom) }
        (dialog as? BottomSheetDialog?)?.behavior?.setState(BottomSheetBehavior.STATE_EXPANDED)
        dialog?.window?.setBackgroundDrawable(null)
        dialog?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        isCancelable = isCancel
    }

    override val result: RESULT get() = _result.data

    override fun show() {
        show(currAsFM.supportFragmentManager, null)
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

private class TopNoticeDialog(private val view: View) : IPermissionWithDialog {
    private val container: FrameLayout? get() = currAsFM.findViewById(android.R.id.content)
    private val registry = LifecycleRegistry(this)

    override fun show() {
        val vg = container ?: return
        registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        vg.addView(
            view, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT
            )
        )
        registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun dismiss() {
        val vg = container ?: return
        registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        vg.removeView(view)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }

    override val lifecycle: Lifecycle get() = registry
}
