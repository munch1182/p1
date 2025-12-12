package com.munch1182.android.lib.helper.result

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import com.munch1182.android.lib.helper.currAsFM
import com.munch1182.android.lib.helper.result.ResultHelper.ContractResultHelper
import com.munch1182.android.lib.helper.result.ResultHelper.JudgeResultHelper
import com.munch1182.android.lib.helper.result.ResultHelper.PermissionResultHelper

fun <I, O> contract(contract: ActivityResultContract<I, O>, input: I) = ContractResultHelper(currAsFM, contract, input)
fun intent(intent: Intent) = ContractResultHelper(currAsFM, ActivityResultContracts.StartActivityForResult(), intent)
fun judge(judge: (Context) -> Boolean, intent: Intent) = JudgeResultHelper(currAsFM, judge, intent)
fun permission(permission: List<String>) = PermissionResultHelper(currAsFM, permission)
fun permission(vararg permission: String) = PermissionResultHelper(currAsFM, permission.toList())

fun Map<String, PermissionResult>.isAllGranted() = values.all { it.isGranted }