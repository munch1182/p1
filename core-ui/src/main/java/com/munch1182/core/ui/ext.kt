package com.munch1182.core.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

/**
 * 将 Flow 转为 StateFlow，使用默认的 WhileSubscribed(5000)。
 */
fun <T> Flow<T>.stateInWithStarted(
    scope: CoroutineScope, initValue: T,
    started: SharingStarted = SharingStarted.WhileSubscribed(5000)
) = stateIn(
    scope, started, initValue
)
