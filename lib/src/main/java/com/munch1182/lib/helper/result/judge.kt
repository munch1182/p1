package com.munch1182.lib.helper.result

import android.content.Context
import android.content.Intent
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal class JudgeTaskCtx(fm: FragmentActivity) : ContractTaskCtx<Boolean>(fm) {
    override val result: Boolean
        get() = super.result ?: false
}

internal class JudgeCheckTask(private val judge: (Context) -> Boolean) : ResultTask<JudgeTaskCtx> {
    override suspend fun execute(ctx: JudgeTaskCtx): ResultTaskResult {
        if (judge(ctx.fm)) {
            ctx.updateResult(true)
            return ResultTaskResult.Skip
        } else {
            return ResultTaskResult.Success
        }
    }
}

internal class JudgeRequestTask(private val intent: Intent, private val provider: ContractDialogProvider?) : ResultTask<JudgeTaskCtx> {
    override suspend fun execute(ctx: JudgeTaskCtx): ResultTaskResult {
        suspendCancellableCoroutine { acc ->
            ResultHelper(ctx.fm).intent(intent).onDialog(provider).request { acc.resume(it) }
        }
        return ResultTaskResult.Success
    }
}