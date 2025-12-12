package com.munch1182.android.lib.base

import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentDialog

@FunctionalInterface
fun interface OnResultListener<T> {
    fun onResult(result: T)
}

@FunctionalInterface
fun interface OnUpdateListener<T> {
    fun onUpdate(update: T)
}

@FunctionalInterface
fun interface OnChangeListener<T> {
    fun onUpdate(curr: T, prev: T?)
}

@FunctionalInterface
fun interface DialogProvider {
    fun onCreateDialog(ctx: Context): ComponentDialog?
}

@FunctionalInterface
fun interface ViewInflaterProvider {
    fun onCreateView(li: LayoutInflater, parent: ViewGroup?): View?
}

@FunctionalInterface
fun interface ViewCtxProvider {
    fun onCreateView(ctx: Context): View?
}

@FunctionalInterface
fun interface OnStateValueChangeListener<STATE, VALUE> {
    fun onStateChange(state: STATE, value: VALUE)
}

@FunctionalInterface
fun interface DialogViewCtxProvider {
    fun onCreateView(ctx: Context, dialog: DialogInterface?): View?
}

interface Releasable {
    fun release()
}

fun Array<Releasable>.release() {
    forEach { it.release() }
}