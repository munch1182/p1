package com.munch1182.lib.result

import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.fragment.app.FragmentActivity
import com.munch1182.lib.base.Need
import com.munch1182.lib.base.toArray
import com.munch1182.lib.helper.ActivityCurrHelper
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

suspend fun PermissionHelper.Request.request() = suspendCoroutine { c -> request { c.resume(it) } }
suspend fun IntentHelper.Request.request() = suspendCoroutine { c -> request { c.resume(it) } }
suspend fun JudgeIntentHelper.Request.request() = suspendCoroutine { c -> request { c.resume(it) } }

private val act: FragmentActivity
    get() = ActivityCurrHelper.currAsFA!!

@Need("[ActivityCurrHelper.register()]")
fun <I, O> contract(contract: ActivityResultContract<I, O>) = ContractHelper.init(act).contract(contract)
fun permission(vararg permission: String) = PermissionHelper.init(act).permission(permission.toArray())
fun PermissionHelper.permission(vararg permission: String) = permission(permission.toArray())

// 用于不同版本权限的情形
fun permission(permission: () -> Array<String>) = PermissionHelper.init(act).permission(permission())
fun PermissionHelper.permission(permission: () -> Array<String>) = permission(permission())

// dialogBefore不适合这种语法
fun intent(intent: Intent) = IntentHelper.init(act).intent(intent)
fun judge(judge: Judge) = JudgeIntentHelper.init(act).judge(judge)

fun PermissionHelper.Dialog.ifAllGrant() = ifOk { it.all { p -> p.value.isGranted } }
fun PermissionHelper.Request.ifAllGrant() = ifOk { it.all { p -> p.value.isGranted } }

fun JudgeIntentHelper.Request.ifOk() = ifOk { it }
fun JudgeIntentHelper.Request.ifNotOk() = ifOk { !it }

fun ContactHelper.permission(vararg permissions: String) = permission(permissions.toArray())
fun ContactHelper.permission(permission: () -> Array<String>) = permission(permission())
