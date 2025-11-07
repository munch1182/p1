package com.munch1182.lib.helper.result

import androidx.activity.result.contract.ActivityResultContract
import androidx.fragment.app.FragmentActivity
import com.munch1182.lib.base.withUI
import com.munch1182.lib.helper.AllowDeniedDialog
import com.munch1182.lib.helper.isAllow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

sealed class ContractTarget : DialogTarget {
    object BeforeRequest : ContractTarget()
    object AfterRequest : ContractTarget()

    override val isMust: Boolean get() = false

    override fun toString() = when (this) {
        AfterRequest -> "AfterRequest"
        BeforeRequest -> "BeforeRequest"
    }
}

fun interface ContractDialogProvider {
    fun onProvider(target: ContractTarget): AllowDeniedDialog?
}


internal open class ContractTaskCtx<O>(val fm: FragmentActivity) : ResultTaskContext {
    private var _result: O? = null
    open val result get() = _result
    fun updateResult(result: O?) {
        _result = result
    }
}

internal class ContractDialogTask<O>(
    private val target: ContractTarget, private val provider: ContractDialogProvider?
) : ResultTask<ContractTaskCtx<O>> {
    override suspend fun execute(ctx: ContractTaskCtx<O>): ResultTaskResult {
        val dialog = withUI { provider?.onProvider(target) }
        if (dialog == null) return if (target.isMust) ResultTaskResult.Skip else ResultTaskResult.Success
        return if (withUI { !dialog.isAllow() }) ResultTaskResult.Skip else ResultTaskResult.Success
    }
}

internal class ContractRequestTask<I, O>(private val contract: ActivityResultContract<I, O>, private val input: I) : ResultTask<ContractTaskCtx<O>> {
    override suspend fun execute(ctx: ContractTaskCtx<O>): ResultTaskResult {
        val result = withUI {
            suspendCancellableCoroutine { ca ->
                val frag = ResultFragment(contract, input) { ca.resume(it) }
                frag.start(ctx.fm)
            }
        }
        ctx.updateResult(result)
        return ResultTaskResult.Success
    }

}

