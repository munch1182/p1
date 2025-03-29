package com.munch1182.lib.helper.result

import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.munch1182.lib.base.resCollapse
import com.munch1182.lib.helper.currFM
import com.munch1182.lib.helper.result.JudgeHelper.OnJudge

fun <I, O> contract(contract: ActivityResultContract<I, O>) = ContractHelper.init(currFM).contract(contract)

fun permission(vararg permission: String) = PermissionHelper.init(currFM).permission(permission.copyInto(Array(permission.size) { "" }))

// 用于多版本权限
fun permission(permission: () -> Array<String>) = PermissionHelper.init(currFM).permission(permission())

fun intent(i: Intent) = IntentHelper.init(currFM).intent(i)

fun judge(judge: OnJudge) = JudgeHelper.init(currFM).judge(judge)


suspend fun <I, O> ContractHelper.Request<I, O>.request() = request(resCollapse())
suspend fun PermissionHelper.Request.request() = request(resCollapse())
suspend fun JudgeHelper.Request.request() = request(resCollapse())
