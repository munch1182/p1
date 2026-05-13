package com.munch1182.core.android

interface ResultRunner<T> {
    suspend fun run(): Result<T>
}