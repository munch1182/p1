package com.munch1182.lib.helper.dialog

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.munch1182.lib.base.OnResultListener
import com.munch1182.lib.base.onDestroyed

interface ResultDialog<RESULT> : LifecycleOwner {
    /**
     * 将dialog的值转为静态的值，当dialog销毁时，获取该值即dialog的结果回调
     * 相当于为所有dialog添加了listener
     */
    val result: RESULT?
    fun show()
}

fun <RESULT> ResultDialog<RESULT>.onResult(onResult: OnResultListener<RESULT?>) {
    lifecycle.onDestroyed { onResult.onResult(result) }
}

@FunctionalInterface
fun interface DialogProvider<DIALOG : ResultDialog<RESULT>, RESULT> {
    fun onCreateDialog(ctx: Context): DIALOG
}