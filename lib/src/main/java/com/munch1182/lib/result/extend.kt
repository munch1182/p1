package com.munch1182.lib.result

import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.munch1182.lib.result.ResultHelper.IResultDialog

fun FragmentActivity.with(vararg permission: String) = ResultHelper.init(this).with(*permission)
fun FragmentActivity.with(intent: Intent) = ResultHelper.init(this).with(intent)
fun FragmentActivity.with(judge: (Context) -> Boolean, intent: Intent) =
    ResultHelper.init(this).with(judge, intent)

fun Fragment.with(vararg permission: String) = ResultHelper.init(this).with(*permission)
fun Fragment.with(intent: Intent) = ResultHelper.init(this).with(intent)
fun Fragment.with(judge: (Context) -> Boolean, intent: Intent) =
    ResultHelper.init(this).with(judge, intent)


fun ResultHelper.Builder.explainPermission(explain: (request: ResultHelper.PERMISSION) -> IResultDialog) =
    explain {
        if (it is ResultHelper.PERMISSION) {
            return@explain explain.invoke(it)
        }
        throw UnsupportedOperationException()
    }

fun ResultHelper.Builder.explainIntent(explain: (request: ResultHelper.INTENT) -> IResultDialog) =
    explain {
        if (it is ResultHelper.INTENT) {
            return@explain explain.invoke(it)
        }
        throw UnsupportedOperationException()
    }