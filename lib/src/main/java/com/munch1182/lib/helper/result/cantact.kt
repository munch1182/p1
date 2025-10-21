package com.munch1182.lib.helper.result

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.munch1182.lib.base.launchIO
import com.munch1182.lib.helper.AllowDeniedDialog
import com.munch1182.lib.helper.result.ResultHelper.PermissionDialogTime
import com.munch1182.lib.helper.result.ResultHelper.PermissionResult
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * 如果当前请求的结果[any]返回true，则执行下一个请求，否则不会执行下一个而是回调false
 *
 * 此方法不会调用请求，手动调用[ContactResultHelper.request]时才会实际执行
 */
fun ResultHelper.PermissionsResultHelper.ifAny(any: (Map<String, PermissionResult>) -> Boolean) = ContactResultHelper.ContactPermissionsResultHelper(this, ContactResultHelper(fm)).ifAny(any)

fun ResultHelper.JudgeHelper.ifTrue() = ContactResultHelper.ContactJudgeHelper(this, ContactResultHelper(fm)).ifTrue()
fun ResultHelper.JudgeHelper.ifFalse() = ContactResultHelper.ContactJudgeHelper(this, ContactResultHelper(fm)).ifFalse()

fun ResultHelper.IntentResultHelper.ifOk(any: (ActivityResult) -> Boolean) = ContactResultHelper.ContactIntentResultHelper(this, ContactResultHelper(fm)).ifOk(any)

/**
 * 组合所有的请求，使其可以一同使用
 *
 * 当前一请求满足条件后，即判断下一条件；任一条件失败，则回调false，否则，回调true；
 * 方法与[ResultHelper]中同名，使其无感使用；
 *
 * @see request
 * @see addTask
 */
class ContactResultHelper(private val fm: FragmentActivity, private val tasks: MutableList<suspend () -> Boolean> = mutableListOf()) {

    fun permission(permissions: Array<String>) = ContactPermissionsResultHelper(ResultHelper.PermissionsResultHelper(fm, permissions), this)
    fun intent(intent: Intent) = ContactIntentResultHelper(ResultHelper.IntentResultHelper(fm, intent), this)
    fun judge(judge: (Context) -> Boolean, intent: Intent) = ContactJudgeHelper(ResultHelper.JudgeHelper(fm, judge, intent), this)

    fun request(callback: (Boolean) -> Unit) {
        fm.lifecycleScope.launchIO {
            for (function in tasks) {
                if (!function.invoke()) {
                    callback.invoke(false)
                    return@launchIO
                }
            }
            callback.invoke(true)
        }
    }

    internal fun addTask(task: suspend () -> Boolean): ContactResultHelper {
        tasks.add(task)
        return this
    }

    class ContactPermissionsResultHelper(internal val permissionsHelper: ResultHelper.PermissionsResultHelper, internal val contact: ContactResultHelper) {

        fun onDialog(show: (PermissionDialogTime, Array<String>) -> AllowDeniedDialog?): ContactPermissionsResultHelper {
            permissionsHelper.onDialog(show)
            return this
        }

        fun ifAny(any: (Map<String, PermissionResult>) -> Boolean) = contact.addTask {
            val result = suspendCancellableCoroutine { ctx -> permissionsHelper.request { r -> ctx.resume(r) } }
            any.invoke(result)
        }
    }

    class ContactIntentResultHelper(private val intentHelper: ResultHelper.IntentResultHelper, private val contact: ContactResultHelper) {

        fun onDialog(show: () -> AllowDeniedDialog?): ContactIntentResultHelper {
            intentHelper.onDialog(show)
            return this
        }

        fun ifOk(ok: (ActivityResult) -> Boolean) = contact.addTask {
            val result = suspendCancellableCoroutine { ctx -> intentHelper.request { r -> ctx.resume(r) } }
            ok.invoke(result)
        }
    }

    class ContactJudgeHelper(internal val judgeHelper: ResultHelper.JudgeHelper, private val contact: ContactResultHelper) {
        fun onDialog(show: () -> AllowDeniedDialog?): ContactJudgeHelper {
            judgeHelper.onDialog(show)
            return this
        }

        fun ifFalse() = contact.addTask {
            !suspendCancellableCoroutine { ctx -> judgeHelper.request { r -> ctx.resume(r) } }
        }

        fun ifTrue() = contact.addTask {
            suspendCancellableCoroutine { ctx -> judgeHelper.request { r -> ctx.resume(r) } }
        }
    }

}