package com.munch1182.p1.dialog

import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.munch1182.core.android.IResultPrompt
import com.munch1182.core.common.INotice
import com.munch1182.p1.base.currAsFragmentActivityOrThrow
import com.munch1182.p1.base.currOrThrow

/**
 * 最为dialog的创造工厂, 提供app所有需要的dialog
 *
 * 使用dialog时, 不应该直接使用构造, 而是使用此类的方法, 既可以简化创建, 又方便后期修改
 */
object Dialog {

    /**
     * 创建一个普通的提示dialog, 样式默认
     */
    fun newYesNoDialog(
        msg: String, //
        title: String = "提示", //
        ctx: FragmentActivity = currAsFragmentActivityOrThrow, //
        cancelable: Boolean = true, //
        ok: String = ctx.getString(android.R.string.ok), //
        cancel: String = ctx.getString(android.R.string.cancel), //
    ): IResultPrompt<Boolean> =
        CommonDialog(act = ctx, title = title, msg = msg, cancelable = cancelable, ok = ok, cancel = cancel)
}

/**
 * 实现[INotice]
 */
object Notice : INotice {

    override fun toast(message: String) {
        Toast.makeText(currOrThrow, message, Toast.LENGTH_SHORT).show()
    }
}
