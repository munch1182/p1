package com.munch1182.core.android

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.RejectedExecutionHandler
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * 简化[withContext]切换到[Dispatchers.Main]
 */
suspend fun <T> withUi(block: suspend CoroutineScope.() -> T) = withContext(Dispatchers.Main, block)

/**
 * 线程执行器提供者
 */
interface ExecutorProvider {
    /**
     * 创建一个新的队列执行器
     */
    val newSingleExecutor: Executor

    /**
     * 创建一个新的缓存执行器
     *
     * 注意: 处于内存考虑, 不应该允许无线创建线程
     */
    val newCachedExecutor: Executor
}

/**
 * CPU核心数
 */
val cpuCoreCount get() = Runtime.getRuntime().availableProcessors()

/**
 * 实现[ExecutorProvider], 用于简化执行器使用
 */
object ExecutorHelper : ExecutorProvider {
    override val newSingleExecutor: ExecutorService get() = newExecutor(1, 1)
    override val newCachedExecutor: ExecutorService get() = newExecutor()

    /**
     * 创建一个新的执行器
     *
     * 推荐初始线程数:
     * 1. CPU密集型任务 = N+1
     * 2. I/O密集型任务(等待时间 >> 计算时间) = 2×N 起步
     * 3. I/O密集型任务(计算时间不可忽略) = N × (1 + 等待/计算)
     */
    fun newExecutor(
        corePoolSize: Int = cpuCoreCount * 2, //
        maximumPoolSize: Int = cpuCoreCount * 10, //
        keepAliveTime: Long = 5000, //
        keepAliveTimeUnit: TimeUnit = TimeUnit.MILLISECONDS, //
        workQueue: BlockingQueue<Runnable> = ArrayBlockingQueue(100), // 使用有界队列来处理任务堆积过多耗尽内存的问题
        threadFactory: ThreadFactory = NameThreadFactory(), //
        handler: RejectedExecutionHandler = ThreadPoolExecutor.AbortPolicy() // 默认会抛出异常
    ) = ThreadPoolExecutor(
        corePoolSize, maximumPoolSize, keepAliveTime, keepAliveTimeUnit, workQueue, threadFactory, handler
    )
}

/**
 * 带有名称的线程工厂
 */
class NameThreadFactory(private val prefix: String = "named-thread") : ThreadFactory {
    private val idx = AtomicInteger(0)
    override fun newThread(p0: Runnable?): Thread {
        return Thread(p0, "$prefix-${idx.getAndIncrement()}")
    }
}