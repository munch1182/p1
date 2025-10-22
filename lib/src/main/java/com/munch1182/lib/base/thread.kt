package com.munch1182.lib.base

import android.os.Handler
import android.os.HandlerThread
import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

fun LifecycleCoroutineScope.launchIO(
    context: CoroutineContext = EmptyCoroutineContext, start: CoroutineStart = CoroutineStart.DEFAULT, block: suspend CoroutineScope.() -> Unit
) = launch(context + Dispatchers.IO, start, block)

fun LifecycleCoroutineScope.launchMain(
    context: CoroutineContext = EmptyCoroutineContext, start: CoroutineStart = CoroutineStart.DEFAULT, block: suspend CoroutineScope.() -> Unit
) = launch(context + Dispatchers.Main, start, block)

suspend inline fun <T> withUI(noinline block: suspend CoroutineScope.() -> T) = withContext(Dispatchers.Main, block)
fun CoroutineScope.launchIO(
    context: CoroutineContext = EmptyCoroutineContext, start: CoroutineStart = CoroutineStart.DEFAULT, block: suspend CoroutineScope.() -> Unit
) = launch(context + Dispatchers.IO, start, block)

/**
 * 重复提供的协程上下文
 *
 * 当启动新的协程时，会自动取消上一个任务
 */
class ReRunJob : CoroutineScope {
    private var currJob: Job? = null
    override val coroutineContext: CoroutineContext get() = currJob ?: EmptyCoroutineContext

    val newContext get() = newCtx()

    private fun newCtx(): Job {
        cancel()
        val newJob = Job()
        currJob = newJob
        return newJob
    }

    fun newLuncher(
        context: CoroutineContext = Dispatchers.IO, start: CoroutineStart = CoroutineStart.DEFAULT, block: suspend CoroutineScope.() -> Unit
    ) {
        launch(context + newCtx(), start, block)
    }

    fun cancel() {
        currJob?.cancel()
        currJob = null
    }
}

object ThreadHelper : ThreadProvider {
    private val cacheExecutor by lazy { Executors.newCachedThreadPool() }
    override val cache: Executor get() = cacheExecutor
}

interface ThreadProvider {
    // 线程池，用于需要传入线程池本身而不能使用线程多的情形
    val cache: Executor

    // 子线程handler，需要手动销毁
    fun newHandler(name: String = "app"): Handler {
        val ht = HandlerThread(name).apply { start() }
        return Handler(ht.looper)
    }
}