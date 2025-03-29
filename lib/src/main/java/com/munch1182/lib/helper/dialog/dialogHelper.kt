package com.munch1182.lib.helper.dialog

import android.content.Context
import androidx.lifecycle.LifecycleOwner

interface ResultDialog<RESULT> {
    val result: RESULT?
    val owner: LifecycleOwner
    fun show()
}

@FunctionalInterface
fun interface DialogProvider<DIALOG : ResultDialog<RESULT>, RESULT> {
    fun onCreateDialog(ctx: Context): DIALOG
}