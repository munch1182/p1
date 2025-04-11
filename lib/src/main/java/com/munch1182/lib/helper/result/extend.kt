package com.munch1182.lib.helper.result

import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.munch1182.lib.base.resCollapse
import com.munch1182.lib.helper.currAsFM
import com.munch1182.lib.helper.result.JudgeHelper.OnJudge

/**
 * @see com.munch1182.lib.helper.ActivityCurrHelper.register
 */
fun <I, O> contract(contract: ActivityResultContract<I, O>) = ContractHelper.init(currAsFM).contract(contract)

/**
 * @see com.munch1182.lib.helper.ActivityCurrHelper.register
 */
fun permission(vararg permission: String) = PermissionHelper.init(currAsFM).permission(permission.copyInto(Array(permission.size) { "" }))

/**
 * @see com.munch1182.lib.helper.ActivityCurrHelper.register
 */
// 用于多版本权限
fun permission(permission: () -> Array<String>) = PermissionHelper.init(currAsFM).permission(permission())

/**
 * @see com.munch1182.lib.helper.ActivityCurrHelper.register
 */
fun intent(i: Intent) = IntentHelper.init(currAsFM).intent(i)

/**
 * @see com.munch1182.lib.helper.ActivityCurrHelper.register
 */
fun judge(judge: OnJudge) = JudgeHelper.init(currAsFM).judge(judge)

val Map<String, PermissionHelper.Result>.isAllGranted get() = values.all { it.isGranted }

fun PermissionHelper.Req.onGranted(granted: (() -> Unit)? = null) = request { if (it.isAllGranted) granted?.invoke() }
fun JudgeHelper.Req.onTrue(onTrue: (() -> Unit)? = null) = request { if (it) onTrue?.invoke() }
fun JudgeHelper.Req.onFalse(onFalse: (() -> Unit)? = null) = request { if (!it) onFalse?.invoke() }
fun IntentHelper.Req.onData(onIntent: ((Intent) -> Unit)? = null) = request { it.data?.let { i -> onIntent?.invoke(i) } }


suspend fun <I, O> ContractHelper.Request<I, O>.requestNow() = request(resCollapse())
suspend fun PermissionHelper.Req.requestNow() = request(resCollapse())
suspend fun JudgeHelper.Req.requestNow() = request(resCollapse())
