package com.munch1182.lib.helper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.munch1182.lib.AppHelper
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

abstract class BaseReceiver<T>(private val filter: IntentFilter) : BroadcastReceiver(), ARManager<T> by ARDefaultManager() {

    protected open val lock = ReentrantLock()
    protected open var registered = false
        set(value) = lock.withLock { field = value }
        get() = lock.withLock { field }

    open fun registerIfNot(context: Context = AppHelper) {
        if (!registered) register(context)
    }

    open fun unregisterIfNot(context: Context = AppHelper) {
        if (registered) unregister(context)
    }

    open fun register(context: Context = AppHelper) {
        registered = true
        ContextCompat.registerReceiver(context, this, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    open fun unregister(context: Context = AppHelper) {
        registered = false
        context.unregisterReceiver(this)
    }

    protected open fun dispatchOnReceive(action: (T) -> Unit) {
        lock.withLock { forEach(action) }
    }
}