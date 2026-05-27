package com.munch1182.core.android

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
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
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * 简化[withContext]切换到[Dispatchers.Main]
 */
suspend fun <T> withUi(block: suspend CoroutineScope.() -> T) = withContext(Dispatchers.Main, block)

/**
 * 简化完成回调
 */
@Suppress("NOTHING_TO_INLINE")
inline fun CoroutineScope.invokeOnCompletion(noinline handler: (Throwable?) -> Unit) = coroutineContext.job.invokeOnCompletion(handler)

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

/**
 * 管理一个与外部生命周期状态绑定的 [CoroutineScope]。
 *
 * 当外部状态为“活跃”时，创建一个新的作用域（基于给定的父上下文）；
 * 当外部状态变为“非活跃”时，取消当前作用域并置空。
 *
 * @param parentScope 父协程作用域，用于启动内部监听协程，以及作为新建作用域的父上下文。
 * @param isActiveFlow 表示活跃状态的 Flow（例如连接状态的流），当值为 true 时视为活跃，false 视为非活跃。
 * @param scopeContextAddition 额外添加到作用域中的上下文元素
 *
 * 最终创建的作用域为：`CoroutineScope(parentScope.coroutineContext + scopeContextAddition + Job())`
 *
 * 使用示例（BLE 连接）：
 * ```
 * class UserSession(scope: CoroutineScope) {
 *      private val _isLoggedIn = MutableStateFlow(false)
 *      private val sessionScope = LifecycleBoundScope(scope, _isLoggedIn, SupervisorJob())
 *
 *      fun login() { _isLoggedIn.value = true }
 *      fun logout() { _isLoggedIn.value = false }
 *
 *      // 只有登录后才能执行的网络请求
 *      fun fetchUserProfile(): Deferred<Profile> = sessionScope.currScopeOrEmpty().async {
 *      // 若 logout，此请求自动取消
 *          api.getProfile()
 *      }
 * }
 * ```
 *
 * @see currScopeOrEmpty
 */
class LifecycleBoundScope(
    private val parentScope: CoroutineScope,
    private val isActiveFlow: Flow<Boolean>,
    private val scopeContextAddition: CoroutineContext = EmptyCoroutineContext
) {

    companion object {
        /**
         * 一个已经取消的协程作用域，在其上启动的任何协程都不会执行。
         * 可作为“空作用域”使用，避免 null 检查。
         */
        private val EmptyScope = CoroutineScope(Job().apply { cancel() })
    }

    private val mutex = Any()
    private var currentScope: CoroutineScope? = null

    init {
        parentScope.launch {
            isActiveFlow.collect { isActive ->
                synchronized(mutex) {
                    if (isActive) {
                        // 活跃：如果已有作用域则先取消（避免重复）
                        currentScope?.cancel()
                        currentScope = CoroutineScope(parentScope.coroutineContext + scopeContextAddition + Job())
                    } else {
                        // 非活跃：取消并置空
                        currentScope?.cancel()
                        currentScope = null
                    }
                }
            }
        }
    }

    /**
     * 获取当前活跃的作用域。
     * @throws IllegalStateException 如果当前状态为非活跃（未连接/未登录等）
     */
    fun currScopeOrException() = synchronized(mutex) { currentScope ?: error("LifecycleBoundScope: not active") }

    /**
     * 获取当前活跃的作用域，如果当前不活跃则返回一个空的 [CoroutineScope]。
     * 空作用域上启动的协程会立即取消，不会执行。
     *
     * ## 注意
     * 若使用 async 并 await()，会抛出 CancellationException，而不是预期的不执行。
     *
     * 使用示例：
     * ```
     * val scope = boundScope.currScopeOrEmpty()
     * scope.launch {
     *     // 如果当前不活跃，这里的代码永远不会执行
     * }
     * ```
     */
    fun currScopeOrEmpty() = synchronized(mutex) { currentScope } ?: EmptyScope
}