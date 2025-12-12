package com.munch1182.android.lib.helper.result

import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.munch1182.android.lib.base.isCanAsk
import com.munch1182.android.lib.base.isGranted
import com.munch1182.android.lib.base.launchIO
import com.munch1182.android.lib.base.withUI
import com.munch1182.android.lib.helper.AllowDeniedDialog
import com.munch1182.android.lib.helper.IDialog
import com.munch1182.android.lib.helper.isAllow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

sealed class PermissionResult {
    object Granted : PermissionResult()
    object Denied : PermissionResult()
    object NeverAsk : PermissionResult()

    override fun toString() = when (this) {
        Granted -> "Granted"
        Denied -> "Denied"
        NeverAsk -> "NeverAsk"
    }

    val isGranted get() = this is Granted
    val isDenied get() = this is Denied
    val isNeverAsk get() = this is NeverAsk
}

sealed class PermissionTarget : DialogTarget {
    object ForRequestFirst : PermissionTarget()
    object ForRequestDenied : PermissionTarget()
    object ForRequestNeverAsk : PermissionTarget()

    /**
     * 如果不是第一次请求，都必须显示dialog允许之后才能继续请求
     */
    override val isMust: Boolean get() = this !is ForRequestFirst

    override fun toString() = when (this) {
        ForRequestFirst -> "ForRequestFirst"
        ForRequestDenied -> "ForRequestDenied"
        ForRequestNeverAsk -> "ForRequestNeverAsk"
    }
}

interface IPermissionWithDialog : IDialog

fun interface PermissionDialogProvider {
    /**
     * 提供一个[AllowDeniedDialog]用于在请求前提示与允许
     */
    fun onProvider(target: PermissionTarget, permission: List<String>): AllowDeniedDialog?
}

fun interface PermissionWithDialogProvider {
    /**
     * 提供一个[IPermissionWithDialog]用于在请求时同步显示
     */
    fun onProvider(permission: List<String>): IPermissionWithDialog?
}

internal data class PermissionTaskCtx(val fm: FragmentActivity, val permission: List<String>) : ResultTaskContext {

    private var _isRequested: Boolean = false

    /**
     * 是否已执行过请求
     */
    val isRequested get() = _isRequested

    private val _result = mutableMapOf<String, PermissionResult>()

    /**
     * 只有[denied]可用于请求
     */
    val denied get() = _result.filterValues { it == PermissionResult.Denied }.keys.toList()
    val neverAsk get() = _result.filterValues { it == PermissionResult.NeverAsk }.keys.toList()
    val result: Map<String, PermissionResult> get() = _result

    private var _onPermissionRequest: (suspend (Boolean) -> Unit)? = null
    val onPermissionRequest get() = _onPermissionRequest

    fun setOnPermissionShow(onPermissionRequest: (suspend (Boolean) -> Unit)?) {
        this._onPermissionRequest = onPermissionRequest
    }

    fun updateResult(result: Map<String, PermissionResult>) {
        _result.clear()
        _result.putAll(result)
    }

    /**
     * 已执行过权限，更改状态
     */
    fun requested() {
        _isRequested = true
    }

    /**
     * 获取[target]所指向的权限列表
     */
    fun targetWithList(target: PermissionTarget) = when (target) {
        PermissionTarget.ForRequestDenied -> denied
        PermissionTarget.ForRequestFirst -> permission
        PermissionTarget.ForRequestNeverAsk -> neverAsk
    }
}

/**
 * 实际执行权限请求的任务
 */
internal class PermissionRequestTask() : ResultTask<PermissionTaskCtx> {
    override suspend fun execute(ctx: PermissionTaskCtx): ResultTaskResult {
        if (ctx.denied.isNotEmpty()) {
            withUI {
                suspendCancellableCoroutine { ca ->
                    val frag = ResultFragment(ActivityResultContracts.RequestMultiplePermissions(), ctx.denied.toTypedArray()) {
                        ctx.requested()
                        ctx.fm.lifecycleScope.launchIO { ctx.onPermissionRequest?.invoke(false) }
                        ca.resume(it)
                    }
                    ctx.fm.lifecycleScope.launchIO { ctx.onPermissionRequest?.invoke(true) }
                    frag.start(ctx.fm)
                }
            }
            return ResultTaskResult.Success
        } else if (ctx.neverAsk.isEmpty()) {
            return ResultTaskResult.Skip
        } else {
            return ResultTaskResult.Success
        }
    }
}

/**
 * 执行权限判断的任务，结果从此处更新
 */
internal class PermissionCheckTask : ResultTask<PermissionTaskCtx> {
    override suspend fun execute(ctx: PermissionTaskCtx): ResultTaskResult {
        val result = ctx.permission.associateWith {
            when {
                it.isGranted() -> PermissionResult.Granted
                ctx.isRequested && !it.isCanAsk(ctx.fm) -> PermissionResult.NeverAsk
                else -> PermissionResult.Denied
            }
        }
        ctx.updateResult(result)
        return ResultTaskResult.Success
    }
}

/**
 * 处理[AllowDeniedDialog]的显示与判断
 */
internal class PermissionDialogTask(
    private val target: PermissionTarget, private val provider: PermissionDialogProvider?
) : ResultTask<PermissionTaskCtx> {
    override suspend fun execute(ctx: PermissionTaskCtx): ResultTaskResult {
        val targetData = ctx.targetWithList(target)
        if (targetData.isEmpty()) return ResultTaskResult.Success
        val dialog = withUI { provider?.onProvider(target, targetData) }
        if (dialog == null) return if (target.isMust) ResultTaskResult.Skip else ResultTaskResult.Success
        return if (withUI { dialog.isAllow() }) ResultTaskResult.Success else ResultTaskResult.Skip
    }
}

/**
 * 处理[IPermissionWithDialog]的显示与判断
 */
internal class PermissionWithRequestTask(private val provider: PermissionWithDialogProvider?) : ResultTask<PermissionTaskCtx> {
    override suspend fun execute(ctx: PermissionTaskCtx): ResultTaskResult {
        val targetData = ctx.targetWithList(PermissionTarget.ForRequestDenied)
        if (targetData.isEmpty()) return ResultTaskResult.Success
        val dialog = withUI { provider?.onProvider(ctx.denied) } ?: return ResultTaskResult.Success
        ctx.setOnPermissionShow {
            if (it) withUI { dialog.show() } else withUI { dialog.dismiss() }
        }
        return ResultTaskResult.Success
    }
}

/**
 * 处理权限被永久拒绝时的[intent]跳转
 */
internal class PermissionIntentTask(private val intent: Intent?) : ResultTask<PermissionTaskCtx> {
    override suspend fun execute(ctx: PermissionTaskCtx): ResultTaskResult {
        intent ?: return ResultTaskResult.Success
        val targetData = ctx.targetWithList(PermissionTarget.ForRequestNeverAsk)
        if (targetData.isEmpty()) return ResultTaskResult.Success
        suspendCancellableCoroutine { acc ->
            ResultHelper(ctx.fm).intent(intent).request { acc.resume(it) }
        }
        return ResultTaskResult.Success
    }
}
