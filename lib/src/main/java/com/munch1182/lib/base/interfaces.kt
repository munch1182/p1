package com.munch1182.lib.base

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.activity.ComponentDialog
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@FunctionalInterface
fun interface OnResultListener<T> {
    fun onResult(result: T)
}

suspend fun <T> resCollapse() = suspendCoroutine { c -> OnResultListener<T> { c.resume(it) } }

@FunctionalInterface
fun interface DialogProvider {
    fun onCreateDialog(ctx: Context): ComponentDialog?
}

@FunctionalInterface
fun interface ViewInflaterProvider {
    fun onCreateView(li: LayoutInflater, parent: android.view.ViewGroup?): View?
}

@FunctionalInterface
fun interface ViewCtxProvider {
    fun onCreateView(ctx: Context): View?
}

@FunctionalInterface
fun interface OnStateChangeListener<T> {
    fun onStateChange(state: T)
}

@FunctionalInterface
fun interface OnStateValueChangeListener<STATE, VALUE> {
    fun onStateChange(state: STATE, value: VALUE)
}