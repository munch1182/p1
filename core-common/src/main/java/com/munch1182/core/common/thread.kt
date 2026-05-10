package com.munch1182.core.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * launch附加[Dispatchers.IO]线程
 */
fun CoroutineScope.launchIO(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
) = launch(context + Dispatchers.IO, start, block)

/**
 * launch附加[Dispatchers.Main]线程
 *
 * 如果不直接支持[Dispatchers.Main]的平台，则需要先手动支持才能调用此方法
 */
fun CoroutineScope.launchMain(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
) = launch(context + Dispatchers.Main, start, block)