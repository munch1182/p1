package com.munch1182.lib.result

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.munch1182.lib.base.NeedInitialize
import com.munch1182.lib.helper.ActivityCurrHelper
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private fun Array<out String>.into() = Array(this.size) { this[it] }

typealias JudgeFirst = (Context) -> Boolean

@NeedInitialize("ActivityCurrHelper.register")
fun <I, O> contract(contracts: ActivityResultContract<I, O>) =
    ContractHelper.init(ActivityCurrHelper.currAsFA!!).contract(contracts)

@NeedInitialize("ActivityCurrHelper.register")
fun permission(vararg permissions: String) = PermissionHelper.init(ActivityCurrHelper.currAsFA!!).permission(permissions.into())

// 用于需要多版本判断的情形
fun PermissionHelper.permission(permission: () -> Array<String>) = permission(permission.invoke())

@NeedInitialize("ActivityCurrHelper.register")
fun permission(permission: () -> Array<String>) = PermissionHelper.init(ActivityCurrHelper.currAsFA!!).permission(permission.invoke())

@NeedInitialize("ActivityCurrHelper.register")
fun intent(intent: Intent) = IntentHelper.init(ActivityCurrHelper.currAsFA!!).intent(intent)

@NeedInitialize("ActivityCurrHelper.register")
fun intent(clazz: Class<*>) = intent(Intent(ActivityCurrHelper.currAsFA, clazz))

@NeedInitialize("ActivityCurrHelper.register")
fun judgeFirst(judgeFirst: JudgeFirst) = IntentHelper.init(ActivityCurrHelper.currAsFA!!).judgeFirst(judgeFirst)

suspend fun <I, O> ResultExecutor<I, O>.request(): O {
    return suspendCoroutine { c -> request { c.resume(it) } }
}

suspend fun JudgeExecutor.request(): Boolean {
    return suspendCoroutine { c -> request { c.resume(it) } }
}

suspend fun PermissionExecutor.request(): Map<String, PermissionHelper.Result> {
    return suspendCoroutine { c -> request { c.resume(it) } }
}

// 只有该权限存在与判断列表且被拒绝时才需要提示弹窗
fun Map<String, PermissionHelper.Result>.needDeniedDialog(permission: String): Boolean {
    return this[permission]?.isDenied ?: false
}

// 只有该权限存在与判断列表且被永久拒绝时才需要提示弹窗
fun Map<String, PermissionHelper.Result>.needDeniedForeverDialog(permission: String): Boolean {
    return this[permission]?.isDeniedForever ?: false
}
