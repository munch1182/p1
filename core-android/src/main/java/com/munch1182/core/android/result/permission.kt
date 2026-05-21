package com.munch1182.core.android.result

import android.content.Intent
import androidx.fragment.app.FragmentActivity
import com.munch1182.core.android.Log
import com.munch1182.core.android.appSetting
import com.munch1182.core.android.isPermissionGranted
import com.munch1182.core.android.isPermissionShouldRationale

/**
 * 根据权限请求目标和权限返回一个dialog方法的简写
 */
typealias PermissionDialogProvider = suspend (PermissionDialogTarget, Array<String>) -> PermissionPrompt?

/**
 * 执行网络请求, 允许在请求前/后获取回调, 显示dialog, 或者跳转intent去获取权限
 */
class PermissionHelper(private val act: FragmentActivity, permission: Array<String>) {

    companion object {
        private const val TAG = "PermissionHelper"
    }

    private var dialogProvider: PermissionDialogProvider? = null
    private var settingIntent: Intent? = null
    private val result: MutableMap<String, PermissionResult> = permission.associateWith { PermissionResult.Denied }.toMutableMap()
    private var hasEverRequested = false // 是否实际执行过请求， 用于判断是否永久拒绝

    /**
     * 设置在操作前(请求权限/请求跳转设置等)显示的dialog
     */
    fun dialogProvider(provider: PermissionDialogProvider) = apply { dialogProvider = provider }

    /**
     * 如果要跳转设置界面, 设置要跳转的目标
     */
    fun settingIntent(intent: Intent = appSetting) = apply { settingIntent = intent }

    /**
     * 实际执行请求, 返回请求结果
     *
     * 如果在请求前提示并被拒绝，则未被授予的权限会被视为([PermissionResult.Denied]);
     * (实际无法区分[PermissionResult.Denied]和[PermissionResult.NeverAskAgain]);
     * 其余状态下返回的权限是真实状态;
     */
    suspend fun request(): Map<String, PermissionResult> {
        dispatchPermissionWithTarget(hasEverRequested)
        Log.d(TAG, "1. hasEverRequested($hasEverRequested); checkedPermission($result)")
        if (result.isAllGranted()) return result

        val isUserDenied = requestDeniedPermissionReturnDenied()
        Log.d(TAG, "2. requestDeniedPermission: isUserDenied(${isUserDenied}), result($result)")

        if (!isUserDenied) {
            requestNeverAskAgain()
            Log.d(TAG, "3. requestNeverAskAgain: result($result)")
        }

        dispatchPermissionWithTarget(hasEverRequested) // 最后检查权限
        Log.d(TAG, "4. result: $result")
        return result
    }

    /**
     * 处理有永久拒绝的逻辑
     */
    private suspend fun requestNeverAskAgain() {
        val neverAsk = result.filterValues { it == PermissionResult.NeverAskAgain }.keys.toTypedArray()
        Log.d(TAG, "neverAsk(${neverAsk.joinToString()})")
        if (neverAsk.isEmpty()) return
        val intent = settingIntent ?: return
        val dialog = dialogProvider?.invoke(PermissionDialogTarget.NEVER_ASK_AGAIN, neverAsk) ?: return
        val isContinue = dialog.onBeforeRequest()
        if (isContinue) act.requestResult(intent)
        dialog.onAfterRequest()
    }

    /**
     * 处理被拒绝的权限的逻辑
     *
     * @return 如果被拒绝， 返回true
     */
    private suspend fun requestDeniedPermissionReturnDenied(): Boolean {
        var target = PermissionDialogTarget.BEFORE
        while (true) {
            val denied = result.filterValues { it == PermissionResult.Denied }.keys.toTypedArray()

            if (denied.isEmpty()) break // 没有可以处理的权限
            val dialog = dialogProvider?.invoke(target, denied)
            val isContinue = dialog?.onBeforeRequest() ?: target.isBefore // 只有初次请求可以不提示用户而直接进行实际请求
            if (isContinue) {
                act.requestPermissions(denied) // 执行实际请求
                hasEverRequested = true
            }
            dialog?.onAfterRequest()
            if (!isContinue) return true// 如果被拒绝, 停止流程

            target = target.next() ?: break // 流程结束
            dispatchPermissionWithTarget(hasEverRequested)
        }
        return false
    }

    /**
     * @param hasEverRequested 是否已经确切执行过权限请求
     */
    private fun dispatchPermissionWithTarget(hasEverRequested: Boolean) {
        for (permission in result.keys) {
            if (permission.isPermissionGranted()) {
                result[permission] = PermissionResult.Granted
            } else {
                result[permission] = if (hasEverRequested && !permission.isPermissionShouldRationale(act)) {
                    PermissionResult.NeverAskAgain
                } else {
                    PermissionResult.Denied // 如果没有执行过，总是推测为Denied
                }
            }
        }
    }
}

/**
 * 权限请求结果
 */
sealed class PermissionResult {
    /**
     * 已授权
     */
    object Granted : PermissionResult()

    /**
     * 已拒绝
     *
     * @see PermissionHelper.request
     */
    object Denied : PermissionResult()

    /**
     * 已被永久拒绝
     */
    object NeverAskAgain : PermissionResult()

    /**
     * 判断是否是已经授权
     */
    val isGranted: Boolean get() = this is Granted

    override fun toString() = when (this) {
        Denied -> "Denied"
        Granted -> "Granted"
        NeverAskAgain -> "NeverAskAgain"
    }
}

/**
 * 是否所有权限都已被授予
 */
fun Map<String, PermissionResult>.isAllGranted(): Boolean = all { it.value.isGranted }

/**
 * 权限请求的对话框目标
 */
enum class PermissionDialogTarget {
    BEFORE,      // 首次请求
    DENIED,      // 用户拒绝过且尚未勾选“不再询问”
    NEVER_ASK_AGAIN; // 用户勾选“不再询问”

    val isBefore get() = this == BEFORE
    internal fun next() = when (this) {
        BEFORE -> DENIED
        DENIED -> NEVER_ASK_AGAIN
        NEVER_ASK_AGAIN -> null
    }

    override fun toString() = when (this) {
        BEFORE -> "BEFORE"
        DENIED -> "DENIED"
        NEVER_ASK_AGAIN -> "NEVER_ASK_AGAIN"
    }
}

/**
 * 权限请求的提示
 */
interface PermissionPrompt {
    /**
     * 在发起系统权限请求前调用(首次请求/拒绝后的请求等实际请求前的回调)。
     * 返回 true 表示继续请求，false 表示终止当前阶段的流程。
     */
    suspend fun onBeforeRequest(): Boolean

    /**
     * 在系统权限请求完成后调用（无论成功或失败），可用于隐藏加载对话框。
     * 默认空实现。
     */
    suspend fun onAfterRequest() {}
}