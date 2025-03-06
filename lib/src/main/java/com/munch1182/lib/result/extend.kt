package com.munch1182.lib.result

import android.content.Context
import androidx.activity.result.contract.ActivityResultContract
import com.munch1182.lib.helper.ActivityCurrHelper
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private fun Array<out String>.into() = Array(this.size) { this[it] }

typealias JudgeFirst = (Context) -> Boolean

fun <I, O> contract(contracts: ActivityResultContract<I, O>) =
    ContractHelper.init(ActivityCurrHelper.currAsFA!!).contract(contracts)

suspend fun <I, O> ResultExecutor<I, O>.request() {
    suspendCoroutine { c -> request { c.resume(it) } }
}
