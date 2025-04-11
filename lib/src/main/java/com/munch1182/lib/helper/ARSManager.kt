package com.munch1182.lib.helper

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

interface ARSManager<T> : Iterable<T> {
    fun add(t: T): ARSManager<T>
    fun remove(t: T): ARSManager<T>
}

class ARSDefaultManager<T> : ARSManager<T> {
    private val _list = mutableListOf<T>()

    override fun add(t: T): ARSManager<T> {
        if (!_list.contains(t)) _list.add(t)
        return this
    }

    override fun remove(t: T): ARSManager<T> {
        if (_list.contains(t)) _list.remove(t)
        return this
    }

    override fun iterator() = _list.iterator()
}

class ARSDefaultSyncManager<T> : ARSManager<T> {
    private val lock = ReentrantLock()
    private val manager = ARSDefaultManager<T>()

    override fun add(t: T): ARSManager<T> {
        lock.withLock { manager.add(t) }
        return this
    }

    override fun remove(t: T): ARSManager<T> {
        lock.withLock { manager.remove(t) }
        return this
    }

    override fun iterator() = lock.withLock { manager.iterator() }
}

fun <T> ARSManager<T>.onResumePause(owner: LifecycleOwner, l: T) {
    owner.lifecycle.addObserver(object : DefaultLifecycleObserver {
        override fun onResume(owner: LifecycleOwner) {
            super.onResume(owner)
            add(l)
        }

        override fun onPause(owner: LifecycleOwner) {
            super.onPause(owner)
            remove(l)
        }
    })
}

fun <T> ARSManager<T>.onCreateDestroy(owner: LifecycleOwner, l: T) {
    owner.lifecycle.addObserver(object : DefaultLifecycleObserver {
        override fun onCreate(owner: LifecycleOwner) {
            super.onCreate(owner)
            add(l)
        }

        override fun onDestroy(owner: LifecycleOwner) {
            super.onDestroy(owner)
            remove(l)
        }
    })
}