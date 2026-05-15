package com.munch1182.core.android

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 简化[withContext]切换到[Dispatchers.Main]
 */
suspend fun <T> withUi(block: suspend CoroutineScope.() -> T) = withContext(Dispatchers.Main, block)