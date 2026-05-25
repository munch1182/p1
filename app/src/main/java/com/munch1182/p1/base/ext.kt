package com.munch1182.p1.base

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

/**
 * 通过[stateIn]将flow转为[kotlinx.coroutines.flow.SharedFlow], 此方法填充了通用的started
 */
fun <T> Flow<T>.stateInWithStarted(
    scope: CoroutineScope, initValue: T,
    started: SharingStarted = SharingStarted.WhileSubscribed(5000)
) = stateIn(
    scope, started, initValue
)