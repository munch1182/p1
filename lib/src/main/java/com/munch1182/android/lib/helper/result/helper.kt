package com.munch1182.lib.helper.result

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.munch1182.lib.base.launchIO

interface IResultHelper<R> {
    fun request(callback: (R) -> Unit)
}

internal interface IExecuteResult<R> {
    suspend fun execute(): R
}

class ResultHelper(internal val fm: FragmentActivity) {
    fun <I, O> contract(contract: ActivityResultContract<I, O>, input: I) = ContractResultHelper(fm, contract, input)
    fun intent(intent: Intent) = ContractResultHelper(fm, ActivityResultContracts.StartActivityForResult(), intent)
    fun judge(judge: (Context) -> Boolean, intent: Intent) = JudgeResultHelper(fm, judge, intent)
    fun permission(permission: List<String>) = PermissionResultHelper(fm, permission)

    class ContractResultHelper<I, O>(internal val fm: FragmentActivity, private val contract: ActivityResultContract<I, O>, private val input: I) : IResultHelper<O?>, IExecuteResult<O?> {
        private var provider: ContractDialogProvider? = null

        fun onDialog(provider: ContractDialogProvider?): ContractResultHelper<I, O> {
            this.provider = provider
            return this
        }

        override fun request(callback: (O?) -> Unit) {
            fm.lifecycleScope.launchIO { callback(execute()) }
        }

        override suspend fun execute(): O? {
            val executor = executor()
            executor.execute()
            return executor.context.result
        }

        private fun executor(): ResultTaskExecutor<ContractTaskCtx<O>> {
            val ctx = ContractTaskCtx<O>(fm)
            val tasks = listOf(
                ContractDialogTask(ContractTarget.BeforeRequest, provider),
                ContractRequestTask(contract, input),
                ContractDialogTask(ContractTarget.AfterRequest, provider),
            )
            return ResultTaskExecutor(ctx, tasks)
        }
    }

    class JudgeResultHelper(internal val fm: FragmentActivity, private val judge: (Context) -> Boolean, private val intent: Intent) : IResultHelper<Boolean>, IExecuteResult<Boolean> {
        private var provider: ContractDialogProvider? = null

        fun onDialog(provider: ContractDialogProvider?): JudgeResultHelper {
            this.provider = provider
            return this
        }

        override fun request(callback: (Boolean) -> Unit) {
            fm.lifecycleScope.launchIO { callback(execute()) }
        }

        private fun executor(): ResultTaskExecutor<JudgeTaskCtx> {
            val ctx = JudgeTaskCtx(fm)
            val tasks = listOf(
                JudgeCheckTask(judge),
                JudgeRequestTask(intent, provider),
                JudgeCheckTask(judge),
            )
            return ResultTaskExecutor(ctx, tasks)
        }

        override suspend fun execute(): Boolean {
            val executor = executor()
            executor.execute()
            return executor.context.result
        }
    }

    class PermissionResultHelper(internal val fm: FragmentActivity, private val permission: List<String>) : IResultHelper<Map<String, PermissionResult>>, IExecuteResult<Map<String, PermissionResult>> {
        private var provider: PermissionDialogProvider? = null
        private var withProvider: PermissionWithDialogProvider? = null
        private var intent: Intent? = null

        fun onIntent(intent: Intent): PermissionResultHelper {
            this.intent = intent
            return this
        }

        fun onDialog(provider: PermissionDialogProvider?): PermissionResultHelper {
            this.provider = provider
            return this
        }

        fun onDialog(provider: PermissionWithDialogProvider?): PermissionResultHelper {
            this.withProvider = provider
            return this
        }

        override fun request(callback: (Map<String, PermissionResult>) -> Unit) {
            fm.lifecycleScope.launchIO { callback(execute()) }
        }

        override suspend fun execute(): Map<String, PermissionResult> {
            val executor = executor()
            executor.execute()
            return executor.context.result
        }

        private fun executor(): ResultTaskExecutor<PermissionTaskCtx> {
            val ctx = PermissionTaskCtx(fm, permission)
            val tasks = listOf(
                PermissionCheckTask(),
                PermissionDialogTask(PermissionTarget.ForRequestFirst, provider),
                PermissionWithRequestTask(withProvider),
                PermissionRequestTask(),
                PermissionCheckTask(),
                PermissionDialogTask(PermissionTarget.ForRequestDenied, provider),
                PermissionWithRequestTask(withProvider),
                PermissionRequestTask(),
                PermissionCheckTask(),
                PermissionDialogTask(PermissionTarget.ForRequestNeverAsk, provider),
                PermissionIntentTask(intent),
                PermissionCheckTask()
            )
            return ResultTaskExecutor(ctx, tasks)
        }
    }
}