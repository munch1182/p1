package com.munch1182.lib.android

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 一个可以 添加/移除/遍历 元素逻辑的管理器
 */
interface ARManager<T> : Iterable<T> {
    /**
     * 获取元素数量
     */
    val size: Int

    /**
     * 添加元素; 如果元素已存在/不能添加的处理逻辑, 需要实现类自行定义;
     */
    fun add(t: T): ARManager<T>

    /**
     * 移除元素
     */
    fun remove(t: T): ARManager<T>

    /**
     * 清空所有元素
     */
    fun clear()
}

/**
 * 将[OnUpdateListener]返回的元素转为一个流; 当流关闭时, 自动移除该回调;
 *
 * 如果重复调用, 需要查看[ARManager]实现类的定义;
 */
fun <T> ARManager<OnUpdateListener<T>>.asFlow() = callbackFlow {
    val lis = OnUpdateListener<T> {
        trySend(it)
    }
    add(lis)
    awaitClose { remove(lis) }
}

class ARDefaultManager<T> : ARManager<T> {
    private val list = mutableListOf<T>()
    override val size: Int
        get() = list.size

    override fun add(t: T): ARManager<T> {
        list.add(t)
        return this
    }

    override fun remove(t: T): ARManager<T> {
        list.remove(t)
        return this
    }


    override fun clear() {
        list.clear()
    }

    override fun iterator() = list.iterator()
}

/**
 * 线程安全 - 使用 CopyOnWriteArrayList, 使用读多写少的场景
 */
class ARSyncManager<T> : ARManager<T> {

    private val list = CopyOnWriteArrayList<T>()
    override val size: Int
        get() = list.size

    override fun add(t: T): ARManager<T> {
        list.add(t)
        return this
    }

    override fun remove(t: T): ARManager<T> {
        list.remove(t)
        return this
    }


    override fun clear() {
        list.clear()
    }

    override fun iterator() = list.iterator()
}

/**
 * 弱引用的ARManager实现
 */
class ARWeakManager<T> : ARManager<T> {
    private val weakMap = java.util.WeakHashMap<T, Boolean>()
    override val size: Int
        get() = weakMap.size

    override fun add(t: T): ARManager<T> {
        weakMap[t] = true
        return this
    }

    override fun remove(t: T): ARManager<T> {
        weakMap.remove(t)
        return this
    }

    override fun clear() {
        weakMap.clear()
    }

    override fun iterator() = weakMap.keys.iterator()
}

/**
 * 弱引用的ARManager实现, 添加删除是线程安全的
 */
class ARSyncWeakManager<T> : ARManager<T> {
    private val weakMap = java.util.Collections.synchronizedMap(java.util.WeakHashMap<T, Boolean>())
    override val size: Int
        get() = weakMap.size

    override fun add(t: T): ARManager<T> {
        weakMap[t] = true
        return this
    }

    override fun remove(t: T): ARManager<T> {
        weakMap.remove(t)
        return this
    }

    override fun clear() {
        weakMap.clear()
    }

    override fun iterator() = synchronized(weakMap) { weakMap.keys.toList().iterator() }
}