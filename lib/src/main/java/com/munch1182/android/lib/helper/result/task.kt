package com.munch1182.lib.helper.result

import kotlinx.coroutines.withTimeout

internal interface ResultTask<C> {
    suspend fun execute(ctx: C): ResultTaskResult
}

internal sealed class ResultTaskResult {
    object Success : ResultTaskResult() // 本任务执行完成，可以执行下一个任务
    object Skip : ResultTaskResult()  // 跳过后续任务, 直接返回结果
    data class Failure(val exception: Throwable) : ResultTaskResult()
}

// 任务执行器
internal class ResultTaskExecutor<T>(internal val context: T, private val tasks: List<ResultTask<T>>, private val timeout: Long = 30000L) {
    suspend fun execute(): ResultTaskResult {
        for (task in tasks) {
            return when (val result = try {
                withTimeout(timeout) { task.execute(context) }
            } catch (e: Exception) {
                ResultTaskResult.Failure(e)
            }) {
                is ResultTaskResult.Failure -> result
                ResultTaskResult.Skip -> ResultTaskResult.Success
                ResultTaskResult.Success -> continue
            }
        }
        return ResultTaskResult.Success
    }
}

// 通用上下文基类
internal interface ResultTaskContext

internal interface DialogTarget {
    /**
     * 当为true时：为了该目标的dialog必须显示，如果不显示，则等同于被拒绝;
     * 否则不显示时等同于被允许
     */
    val isMust: Boolean
}