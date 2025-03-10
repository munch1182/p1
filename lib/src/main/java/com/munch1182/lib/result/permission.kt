package com.munch1182.lib.result

import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.munch1182.lib.base.asPermissionCheck
import com.munch1182.lib.base.asPermissionCheckRationale
import com.munch1182.lib.result.PermissionHelper.DialogCreator
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class PermissionHelper internal constructor(internal val ctx: Context) {

    companion object {

        fun init(act: FragmentActivity) = PermissionHelper(Context(act, act.supportFragmentManager))
        fun init(fg: Fragment) =
            PermissionHelper(Context(fg.requireActivity(), fg.childFragmentManager))
    }

    fun permission(permission: Array<String>) =
        PermissionExecutor(ctx.apply { this.input = permission })


    internal class Context internal constructor(
        val ctx: FragmentActivity,
        fm: FragmentManager,
        var creator: DialogCreator? = null
    ) : ContractHelper.Context<Array<String>, Map<String, Boolean>>(
        fm, ActivityResultContracts.RequestMultiplePermissions()
    )

    sealed class State {
        data object Before : State()
        data object Denied : State()
        data object DeniedForever : State()

        val isBefore: Boolean
            get() = this is Before
        val isDenied: Boolean
            get() = this is Denied
        val isDeniedForever: Boolean
            get() = this is DeniedForever

        val newState: State
            get() = when (this) {
                is Before -> Denied
                is Denied -> DeniedForever
                is DeniedForever -> DeniedForever
            }
    }

    sealed class Result {
        data object Granted : Result()
        data object Denied : Result()
        data object DeniedForever : Result()

        val isGranted: Boolean
            get() = this is Granted
        val isDenied: Boolean
            get() = this is Denied
        val isDeniedForever: Boolean
            get() = this is DeniedForever
    }

    @FunctionalInterface
    fun interface DialogCreator {
        fun create(ctx: android.content.Context, state: State, result: Map<String, Result>): ResultDialog?
    }
}

class PermissionExecutor internal constructor(internal val ctx: PermissionHelper.Context) {

    /**
     * 根据状态显示权限介绍弹窗
     *
     * 不建议在[PermissionHelper.State.Before]中显示弹窗，
     * 因为可能权限已经被永久拒绝但是检测出来依然是[PermissionHelper.Result.Denied]导致空弹窗口而无权限请求的情形
     */
    fun dialogIfNeed(creator: DialogCreator): PermissionExecutor {
        ctx.creator = creator
        return this
    }

    fun request(listener: ContractHelper.OnResultListener<Map<String, PermissionHelper.Result>>) {
        ctx.ctx.lifecycleScope.launch {
            val permission = ctx.input ?: return@launch
            dialogPermissionLogic(PermissionHelper.State.Before, permission.asList(), listener)
        }
    }

    private suspend fun dialogPermissionLogic(
        state: PermissionHelper.State,
        permission: List<String>,
        l: ContractHelper.OnResultListener<Map<String, PermissionHelper.Result>>
    ) {
        // 检查权限, newPermission是需要请求的权限(被永久拒绝的不包含在内)
        val (newPermission, result) = check(permission, !state.isBefore)
        // 如果全授予或者全永久拒绝，则直接回调（此处不处理设置页面跳转）
        if (newPermission.isEmpty()) return l.onResult(result)
        // 如果权限被拒绝后仍被拒绝，则返回。就是全流程只会请求两次，第二次无论是拒绝还是永久拒绝（虽然大部分权限第二次就不会有拒绝选项而是永久拒绝）都不会再继续请求
        if (state.isDeniedForever) return l.onResult(result)
        // 如果还有权限需要申请，则先判断是否需要弹窗提示
        val isContinue = dialogContinue(state, result)
        // 显示弹窗后被取消，则直接返回，若不想被返回，则不要提供弹窗的取消按键(没啥意义)
        // 在实际第一次请求权限前未配置弹窗，仍会继续流程
        // 但在第二次及以后的请求中若无弹窗/或者确认取消，则结束流程
        if (!isContinue) return l.onResult(result)
        // 实际执行权限请求
        requestImpl()
        // 循环执行
        return dialogPermissionLogic(state.newState, newPermission, l)
    }

    private suspend fun requestImpl(): Map<String, Boolean> {
        return suspendCoroutine { c ->
            val frag = PermissionIntentFragment.get(ctx.fm)
            if (frag is PermissionIntentFragment) {
                frag.launchPermission(ctx.input) { c.resume(it) }
            } else {
                c.resume(mapOf())
            }

        }
    }

    private suspend fun dialogContinue(
        state: PermissionHelper.State,
        result: Map<String, PermissionHelper.Result>
    ): Boolean {
        // 只有初次没有提示可以继续，后续如果需要继续请求权限都需要弹窗提示并确认
        val dialog = ctx.creator?.create(ctx.ctx, state, result) ?: return state.isBefore
        return suspendCoroutine { c ->
            dialog.create(ctx.ctx).setOnChoseListener { i -> c.resume(i) }.show()
        }
    }

    private fun check(
        permission: List<String>,
        requested: Boolean = false
    ): Pair<List<String>, Map<String, PermissionHelper.Result>> {
        val map = mutableMapOf<String, PermissionHelper.Result>()
        val newPermission = ArrayList<String>(permission.size)
        permission.forEach {
            map[it] = if (it.asPermissionCheck()) {
                PermissionHelper.Result.Granted
            } else if (requested && !it.asPermissionCheckRationale(ctx.ctx)) {
                PermissionHelper.Result.DeniedForever
            } else {
                newPermission.add(it)
                PermissionHelper.Result.Denied
            }
        }
        return newPermission to map
    }
}

