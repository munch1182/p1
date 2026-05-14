package com.munch1182.p1.dialog

import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.munch1182.core.android.IResultDialog
import com.munch1182.core.common.INotice
import com.munch1182.p1.base.currOrThrow


object Dialog {

    /**
     * 创建一个普通的提示dialog, 样式默认
     */
    fun newYesNoDialog(
        msg: String, //
        title: String = "提示", //
        ctx: FragmentActivity = currOrThrow as? FragmentActivity ?: error("cannot create a dialog on a non-FragmentActivity page"), //
        cancelable: Boolean = true, //
        ok: String = ctx.getString(android.R.string.ok), //
        cancel: String = ctx.getString(android.R.string.cancel), //
    ): IResultDialog<Boolean> =
        CommonDialog(act = ctx, title = title, msg = msg, cancelable = cancelable, ok = ok, cancel = cancel)
}

object Notice : INotice {

    override fun toast(message: String) {
        Toast.makeText(currOrThrow, message, Toast.LENGTH_SHORT).show()
    }
}
