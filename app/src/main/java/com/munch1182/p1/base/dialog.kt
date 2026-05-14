package com.munch1182.p1.base

import android.content.Context
import android.content.DialogInterface
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.munch1182.core.android.ActivityCurrHelper
import com.munch1182.core.android.IResultDialog
import com.munch1182.core.common.INotice

private val currOrThrow get() = ActivityCurrHelper.curr ?: error("use context but ActivityCurrHelper.curr is null")

object Dialog {

    /**
     * 创建一个普通的提示dialog, 样式默认
     */
    fun newYesNoDialog(
        message: String, //
        title: String = "提示", //
        ctx: Context = currOrThrow, //
        cancelable: Boolean = true, //
        ok: String = ctx.getString(android.R.string.ok), //
        cancel: String = ctx.getString(android.R.string.cancel), //
    ): IResultDialog<Boolean> = CommonDialog(title = title, msg = message, cancelable = cancelable, ok = ok, cancel = cancel, ctx = ctx)
}

object Notice : INotice {

    override fun toast(message: String) {
        Toast.makeText(currOrThrow, message, Toast.LENGTH_SHORT).show()
    }
}

class CommonDialog(
    title: String, //
    msg: String, //
    cancelable: Boolean = true, //
    ctx: Context = currOrThrow, //
    ok: String = ctx.getString(android.R.string.ok), //
    cancel: String = ctx.getString(android.R.string.cancel) //
) : AlertDialog(ctx), IResultDialog<Boolean> {

    private var onDismiss: ((Boolean) -> Unit)? = null
    private var last = false

    init {
        val click = DialogInterface.OnClickListener { _, which ->
            last = when (which) {
                BUTTON_POSITIVE -> true
                BUTTON_NEGATIVE -> false
                else -> false
            }
            dismiss()
        }
        setTitle(title)
        setMessage(msg)
        setCancelable(cancelable)
        setButton(BUTTON_POSITIVE, ok, click)
        setButton(BUTTON_NEGATIVE, cancel, click)
        setOnDismissListener { onDismiss?.invoke(last) }
    }

    override fun onShow(onShow: () -> Unit): IResultDialog<Boolean> {
        setOnShowListener { onShow() }
        return this
    }

    override fun onDismiss(onDismiss: (Boolean) -> Unit): IResultDialog<Boolean> {
        this.onDismiss = onDismiss
        return this
    }
}