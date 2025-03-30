package com.munch1182.lib.helper.dialog

import android.content.Context
import androidx.lifecycle.LifecycleOwner

interface ResultDialog<RESULT> : LifecycleOwner {
    val result: RESULT?
    fun show()
}

@FunctionalInterface
fun interface DialogProvider<DIALOG : ResultDialog<RESULT>, RESULT> {
    fun onCreateDialog(ctx: Context): DIALOG
}