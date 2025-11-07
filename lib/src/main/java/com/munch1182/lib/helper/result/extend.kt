package com.munch1182.lib.helper.result

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.munch1182.lib.base.appSetting
import com.munch1182.lib.base.launchIO
import com.munch1182.lib.helper.result.ResultHelper.PermissionResult
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

fun FragmentActivity.permission(vararg permission: String) = ResultHelper(this).permission(permission.asList().toTypedArray())
fun FragmentActivity.permissions(permission: Array<String>) = ResultHelper(this).permission(permission)
fun FragmentActivity.intent(intent: Intent) = ResultHelper(this).intent(intent)
fun FragmentActivity.judge(judge: (Context) -> Boolean, intent: Intent) = ResultHelper(this).judge(judge, intent)

fun <I, O> FragmentActivity.contract(contract: ActivityResultContract<I, O>, input: I) = ResultHelper(this).contract(contract, input)

fun Map<String, PermissionResult>.isAllGranted() = this.values.all { it.isGranted }

/**
 * 当返回的结果有永久拒绝时，
 * 通过[ResultHelper.PermissionsResultHelper.onDialog]提示并允许后跳转[intent]去手动开启
 * 然后回调结果
 */
fun ResultHelper.PermissionsResultHelper.manualIntent(intent: Intent = appSetting()) = PermissionJumpIntentIfNeverAskHelper(this, intent)

/**
 * 如果当前请求的结果返回true，则执行下一个请求，否则不会执行下一个请求也不会回调
 *
 * @see [isAllGranted]
 */
fun ResultHelper.PermissionsResultHelper.ifAll() = ifAny { it.isAllGranted() }

fun PermissionJumpIntentIfNeverAskHelper.ifAny(any: (Map<String, PermissionResult>) -> Boolean) = ResultChainHelper(helper.fm).addTask {
    val result = suspendCancellableCoroutine { ctx -> request { r -> ctx.resume(r) } }
    any.invoke(result)
}

fun PermissionJumpIntentIfNeverAskHelper.ifAll() = ifAny { it.isAllGranted() }
fun ChainPermissionJumpIntentHelper.ifAll() = ifAny { it.isAllGranted() }

fun ResultChainHelper.ChainPermissionsResultHelper.manualIntent(intent: Intent = appSetting()) = ChainPermissionJumpIntentHelper(PermissionJumpIntentIfNeverAskHelper(permissionsHelper, intent), contact)

/**
 * 给[ResultHelper.PermissionsResultHelper]增加跳转到设置界面手动开启权限的功能
 *
 * 需要复用[ResultHelper.PermissionsResultHelper.onDialog]中的[ResultHelper.PermissionDialogTime.NeverAsk]的返回
 */
class PermissionJumpIntentIfNeverAskHelper(internal val helper: ResultHelper.PermissionsResultHelper, private val intent: Intent) {

    fun request(callback: (Map<String, PermissionResult>) -> Unit) {
        helper.request { permissions ->
            val neverAsk = permissions.filter { v -> v.value.isNeverAsk }.keys.toTypedArray()
            if (neverAsk.isEmpty()) return@request callback(permissions)
            helper.fm.lifecycleScope.launchIO {
                if (!helper.showExplainDialogIfNeed(ResultHelper.PermissionDialogTime.NeverAsk, neverAsk)) {
                    return@launchIO callback(permissions)
                }
                ResultHelper(helper.fm).intent(intent).request {
                    val newResult = HashMap<String, PermissionResult>(permissions.size)
                    permissions.keys.forEach { newResult[it] = PermissionResult.fromPermission(it) }
                    callback(newResult)
                }
            }
        }
    }
}

class ChainPermissionJumpIntentHelper(internal val helper: PermissionJumpIntentIfNeverAskHelper, private val contact: ResultChainHelper) {
    fun ifAny(any: (Map<String, PermissionResult>) -> Boolean) = contact.addTask {
        val result = suspendCancellableCoroutine { ctx -> helper.request { r -> ctx.resume(r) } }
        any.invoke(result)
    }
}