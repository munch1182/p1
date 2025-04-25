package com.munch1182.p1.base

import android.content.Context

interface DialogView

interface DialogViewCreator {
    fun createView(ctx: Context?): DialogView?
}

interface DialogShowType {
    fun show(view: DialogView?, showType: DialogShowType)
}