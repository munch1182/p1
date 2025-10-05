package com.munch1182.lib.base

import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

fun LifecycleCoroutineScope.launchIO(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
) = launch(context + Dispatchers.IO, start, block)

fun LifecycleCoroutineScope.launchMain(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
) = launch(context + Dispatchers.Main, start, block)

suspend inline fun <T> withUI(noinline block: suspend CoroutineScope.() -> T) = withContext(Dispatchers.Main, block)
fun CoroutineScope.launchIO(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
) = launch(context + Dispatchers.IO, start, block)

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
        context: CoroutineContext = Dispatchers.IO,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> Unit
    ) {
        launch(context + newCtx(), start, block)
    }

    fun cancel() {
        currJob?.cancel()
        currJob = null
    }
}