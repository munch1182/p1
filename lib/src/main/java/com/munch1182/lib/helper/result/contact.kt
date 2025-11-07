package com.munch1182.lib.helper.result

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.munch1182.lib.base.launchIO

fun <I, O> ResultHelper.ContractResultHelper<I, O>.ifAny(ifAny: (O?) -> Boolean) = ContactResultHelper.ContractPartResultHelper(ContactResultHelper(fm), this).ifAny(ifAny)
fun ResultHelper.JudgeResultHelper.ifAny(ifAny: (Boolean) -> Boolean) = ContactResultHelper.JudgePartResultHelper(ContactResultHelper(fm), this).ifAny(ifAny)
fun ResultHelper.PermissionResultHelper.ifAny(ifAny: (Map<String, PermissionResult>) -> Boolean) = ContactResultHelper.PermissionPartResultHelper(ContactResultHelper(fm), this).ifAny(ifAny)

fun ResultHelper.JudgeResultHelper.ifTrue() = ifAny { it }
fun ResultHelper.PermissionResultHelper.ifAllGranted() = ifAny { it.values.all { p -> p.isGranted } }

interface IfAny<O> {
    fun ifAny(ifAny: (O) -> Boolean): ContactResultHelper
}

fun <O> IfAny<O>.ignoreIf() = ifAny { true }

/**
 * 将原有的[ResultHelper]以无感的方法组合调用与判断
 */
class ContactResultHelper(fm: FragmentActivity) : IResultHelper<Boolean>, IExecuteResult<Boolean> {
    private val helper = ResultHelper(fm)
    private val links = mutableListOf<suspend () -> Boolean>()

    fun <I, O> contract(contract: ActivityResultContract<I, O>, input: I) = ContractPartResultHelper(this, helper.contract(contract, input))

    fun intent(intent: Intent) = ContractPartResultHelper(this, helper.intent(intent))

    fun judge(judge: (Context) -> Boolean, intent: Intent) = JudgePartResultHelper(this, helper.judge(judge, intent))

    fun permission(permission: List<String>) = PermissionPartResultHelper(this, helper.permission(permission))

    /**
     * 实际执行所有请求，任一请求判断为false，则直接回调false
     */
    override fun request(callback: (Boolean) -> Unit) {
        helper.fm.lifecycleScope.launchIO { callback(execute()) }
    }

    override suspend fun execute(): Boolean {
        var result = true
        for (function in links) {
            val res = try {
                function.invoke()
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
            if (!res) {
                result = false
                break
            }
        }
        return result
    }

    class JudgePartResultHelper internal constructor(
        private val link: ContactResultHelper, private val helper: ResultHelper.JudgeResultHelper
    ) : IfAny<Boolean> {

        fun onDialog(provider: ContractDialogProvider?): JudgePartResultHelper {
            helper.onDialog(provider)
            return this
        }

        override fun ifAny(ifAny: (Boolean) -> Boolean): ContactResultHelper {
            link.links.add { ifAny(helper.execute()) }
            return link
        }
    }

    class ContractPartResultHelper<I, O> internal constructor(
        private val link: ContactResultHelper, private val helper: ResultHelper.ContractResultHelper<I, O>
    ) : IfAny<O?> {

        fun onDialog(provider: ContractDialogProvider?): ContractPartResultHelper<I, O> {
            helper.onDialog(provider)
            return this
        }

        override fun ifAny(ifAny: (O?) -> Boolean): ContactResultHelper {
            link.links.add { ifAny(helper.execute()) }
            return link
        }
    }

    class PermissionPartResultHelper internal constructor(
        private val link: ContactResultHelper, private val helper: ResultHelper.PermissionResultHelper
    ) : IfAny<Map<String, PermissionResult>> {

        fun onIntent(intent: Intent): PermissionPartResultHelper {
            helper.onIntent(intent)
            return this
        }

        fun onDialog(provider: PermissionDialogProvider?): PermissionPartResultHelper {
            helper.onDialog(provider)
            return this
        }

        fun onDialog(provider: PermissionWithDialogProvider?): PermissionPartResultHelper {
            helper.onDialog(provider)
            return this
        }

        override fun ifAny(ifAny: (Map<String, PermissionResult>) -> Boolean): ContactResultHelper {
            link.links.add { ifAny(helper.execute()) }
            return link
        }
    }

}