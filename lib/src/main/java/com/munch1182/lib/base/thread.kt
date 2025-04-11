package com.munch1182.lib.base

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

fun LifecycleOwner.launchIO(ctx: CoroutineContext? = null, start: CoroutineStart = CoroutineStart.DEFAULT, block: suspend CoroutineScope.() -> Unit) =
    lifecycleScope.launchIO(ctx, start, block)

fun CoroutineScope.launchIO(ctx: CoroutineContext? = null, start: CoroutineStart = CoroutineStart.DEFAULT, block: suspend CoroutineScope.() -> Unit) = launch(ctx?.let { it + Dispatchers.IO } ?: Dispatchers.IO, start, block)