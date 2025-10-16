package com.munch1182.lib.helper

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

interface ARManager<T> : Iterable<T> {
    fun add(t: T): ARManager<T>
    fun remove(t: T): ARManager<T>
    fun clear(): ARManager<T>
}

class ARDefaultManager<T> : ARManager<T> {
    private val _list = mutableListOf<T>()
    override fun add(t: T): ARManager<T> {
        if (!_list.contains(t)) _list.add(t)
        return this
    }

    override fun remove(t: T): ARManager<T> {
        _list.remove(t)
        return this
    }

    override fun clear(): ARManager<T> {
        _list.clear()
        return this
    }

    override fun iterator() = _list.iterator()

    val size get() = _list.size
}

class ARDefaultSyncManager<T> : ARManager<T> {
    private val lock = ReentrantLock()
    private val manager = ARDefaultManager<T>()

    override fun add(t: T): ARManager<T> {
        lock.withLock { manager.add(t) }
        return this
    }

    override fun remove(t: T): ARManager<T> {
        lock.withLock { manager.remove(t) }
        return this
    }

    override fun iterator() = lock.withLock { manager.iterator() }
    override fun clear() = lock.withLock { manager.clear() }
}

fun <T> ARManager<T>.onCreateDestroy(owner: LifecycleOwner, l: T) {
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
