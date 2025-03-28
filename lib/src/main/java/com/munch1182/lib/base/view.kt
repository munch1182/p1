package com.munch1182.lib.base

import android.content.Context
import android.view.LayoutInflater
import android.view.View

@FunctionalInterface
fun interface ViewInflaterProvider {
    fun onCreateView(li: LayoutInflater, parent: android.view.ViewGroup?): View?
}

@FunctionalInterface
fun interface ViewCtxProvider {
    fun onCreateView(ctx: Context): View?
}

