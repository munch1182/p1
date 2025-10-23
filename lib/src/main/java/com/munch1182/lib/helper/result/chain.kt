package com.munch1182.lib.helper.result

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.munch1182.lib.base.launchIO
import com.munch1182.lib.helper.AllowDeniedDialog
import com.munch1182.lib.helper.result.ResultHelper.PermissionDialogTime
import com.munch1182.lib.helper.result.ResultHelper.PermissionResult
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * 如果当前请求的结果[any]返回true，则执行下一个请求，否则不会执行下一个而是回调false
 *
 * 此方法不会调用请求，手动调用[ResultChainHelper.request]时才会实际执行
 */
fun ResultHelper.PermissionsResultHelper.ifAny(any: (Map<String, PermissionResult>) -> Boolean) = ResultChainHelper.ChainPermissionsResultHelper(this, ResultChainHelper(fm)).ifAny(any)

fun ResultHelper.JudgeResultHelper.ifTrue() = ResultChainHelper.ChainJudgeResultHelper(this, ResultChainHelper(fm)).ifTrue()
fun ResultHelper.JudgeResultHelper.ifFalse() = ResultChainHelper.ChainJudgeResultHelper(this, ResultChainHelper(fm)).ifFalse()

fun ResultHelper.IntentResultHelper.ifOk(any: (ActivityResult) -> Boolean) = ResultChainHelper.ChainIntentResultHelper(this, ResultChainHelper(fm)).ifOk(any)

fun <I, O> ResultHelper.ContractResultHelper<I, O>.ifAny(any: (O?) -> Boolean) = ResultChainHelper.ChainContractResultHelper(this, ResultChainHelper(fm)).ifAny(any)

/**
 * 组合所有的请求，使其可以一同使用
 *
 * 当前一请求满足条件后，即判断下一条件；任一条件失败，则回调false，否则，回调true；
 * 方法与[ResultHelper]中同名，使其无感使用；
 *
 * @see request
 * @see addTask
 */
class ResultChainHelper(private val fm: FragmentActivity, private val tasks: MutableList<suspend () -> Boolean> = mutableListOf()) {

    fun permission(permissions: Array<String>) = ChainPermissionsResultHelper(ResultHelper.PermissionsResultHelper(fm, permissions), this)
    fun intent(intent: Intent) = ChainIntentResultHelper(ResultHelper.IntentResultHelper(fm, intent), this)
    fun judge(judge: (Context) -> Boolean, intent: Intent) = ChainJudgeResultHelper(ResultHelper.JudgeResultHelper(fm, judge, intent), this)
    fun <I, O> contract(contract: ActivityResultContract<I, O>, input: I) = ChainContractResultHelper(ResultHelper.ContractResultHelper(fm, contract, input), this)

    fun request(callback: (Boolean) -> Unit) {
        fm.lifecycleScope.launchIO {
            for (function in tasks) {
                if (!function.invoke()) {
                    callback.invoke(false)
                    return@launchIO
                }
            }
            callback.invoke(true)
        }
    }

    internal fun addTask(task: suspend () -> Boolean): ResultChainHelper {
        tasks.add(task)
        return this
    }

    class ChainPermissionsResultHelper(internal val permissionsHelper: ResultHelper.PermissionsResultHelper, internal val contact: ResultChainHelper) {

        fun onDialog(show: (PermissionDialogTime, Array<String>) -> AllowDeniedDialog?): ChainPermissionsResultHelper {
            permissionsHelper.onDialog(show)
            return this
        }

        fun ifAny(any: (Map<String, PermissionResult>) -> Boolean) = contact.addTask {
            val result = suspendCancellableCoroutine { ctx -> permissionsHelper.request { r -> ctx.resume(r) } }
            any.invoke(result)
        }
    }

    class ChainIntentResultHelper(private val intentHelper: ResultHelper.IntentResultHelper, private val contact: ResultChainHelper) {

        fun onDialog(show: () -> AllowDeniedDialog?): ChainIntentResultHelper {
            intentHelper.onDialog(show)
            return this
        }

        fun ifOk(ok: (ActivityResult) -> Boolean) = contact.addTask {
            val result = suspendCancellableCoroutine { ctx -> intentHelper.request { r -> ctx.resume(r) } }
            ok.invoke(result)
        }
    }

    class ChainJudgeResultHelper(internal val judgeHelper: ResultHelper.JudgeResultHelper, private val contact: ResultChainHelper) {
        fun onDialog(show: () -> AllowDeniedDialog?): ChainJudgeResultHelper {
            judgeHelper.onDialog(show)
            return this
        }

        fun ifFalse() = contact.addTask {
            !suspendCancellableCoroutine { ctx -> judgeHelper.request { r -> ctx.resume(r) } }
        }

        fun ifTrue() = contact.addTask {
            suspendCancellableCoroutine { ctx -> judgeHelper.request { r -> ctx.resume(r) } }
        }
    }

    class ChainContractResultHelper<I, O>(internal val contractHelper: ResultHelper.ContractResultHelper<I, O>, private val contact: ResultChainHelper) {

        fun onDialog(provide: () -> AllowDeniedDialog?): ChainContractResultHelper<I, O> {
            contractHelper.onDialog(provide)
            return this
        }

        fun ifAny(any: (O?) -> Boolean) = contact.addTask {
            val result = suspendCancellableCoroutine { ctx -> contractHelper.request { r -> ctx.resume(r) } }
            any.invoke(result)
        }
    }

}