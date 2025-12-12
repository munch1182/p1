package com.munch1182.android.lib.helper

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.munch1182.android.lib.base.OnUpdateListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

interface ARManager<T> : Iterable<T> {
    /**
     * 添加回调
     */
    fun add(t: T): ARManager<T>

    /**
     * 移除回调
     */
    fun remove(t: T): ARManager<T>

    /**
     * 移除所有回调
     */
    fun clear(): ARManager<T>
}

fun <T> ARManager<OnUpdateListener<T>>.asFlow() = callbackFlow {
    val lis = OnUpdateListener<T> { trySend(it) }
    add(lis)
    awaitClose { remove(lis) }
}

/**
 * 对[ARManager]的默认实现
 */
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

/**
 * 对[ARManager]的同步默认实现
 */
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
