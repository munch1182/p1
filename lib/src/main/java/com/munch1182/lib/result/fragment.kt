package com.munch1182.lib.result

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager


/**
 * 用于处理常用请求，此类不走构造函数，页面重建不会丢失数据
 */
class PermissionIntentFragment : Fragment() {

    companion object {
        private const val TAG = "com.munch1182.lib.result.PermissionIntentFragment"

        internal fun get(fm: FragmentManager): PermissionIntentFragment {
            return fm.findFragmentByTag(TAG) as? PermissionIntentFragment ?: PermissionIntentFragment().apply {
                fm.beginTransaction().add(this, TAG).commitNowAllowingStateLoss()
            }
        }
    }

    private var permissionCallback: ContractHelper.OnResultListener<Map<String, Boolean>>? = null
    private var intentCallback: ContractHelper.OnResultListener<ActivityResult>? = null

    private val permission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        ContractHelper.logger.logStr("PermissionIntentFragment launchPermission callback $it")
        permissionCallback?.onResult(it)
        permissionCallback = null
    }.apply { ContractHelper.logger.logStr("PermissionIntentFragment register RequestMultiplePermissions") }
    private val intent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        ContractHelper.logger.logStr("PermissionIntentFragment launchIntent callback $it")
        intentCallback?.onResult(it)
        intentCallback = null
    }.apply { ContractHelper.logger.logStr("PermissionIntentFragment register StartActivityForResult") }

    fun launchPermission(permissions: Array<String>?, listener: ContractHelper.OnResultListener<Map<String, Boolean>>) {
        ContractHelper.logger.logStr("PermissionIntentFragment launchPermission ${permissions?.joinToString(prefix = "[", postfix = "]")}")
        this.permissionCallback = listener
        this.permission.launch(permissions)
    }

    fun launchIntent(intent: Intent?, listener: ContractHelper.OnResultListener<ActivityResult>) {
        ContractHelper.logger.logStr("PermissionIntentFragment launchIntent $intent")
        this.intentCallback = listener
        this.intent.launch(intent)
    }
}

/**
 * fragment的构造传参会在页面重建(如旋转时)失去数据
 * 解决办法：
 *  1. 锁定方向
 *  2. 旋转等不触发重建
 *  否则会因为重建时fragment会被调用无参构造导致抛出异常
 */
class ContractFragment<I, O>(private val contract: ActivityResultContract<I, O>) : Fragment() {

    companion object {
        private const val TAG = "com.munch1182.lib.result.ContractFragment"

        fun <I, O> get(fm: FragmentManager, contract: ActivityResultContract<I, O>): ContractFragment<I, O> {
            return fm.findFragmentByTag(TAG)?.let {
                // 移除fragment，否则无法更新传入的contract
                if (it.isAdded) fm.beginTransaction().remove(it).commitNowAllowingStateLoss()
                null
                // Fragment直接传参的方法需要保证使用期间页面不被重建
            } ?: ContractFragment(contract).apply {
                fm.beginTransaction().add(this, TAG).commitNowAllowingStateLoss()
            }
        }
    }

    private var callback: ContractHelper.OnResultListener<O>? = null
    private lateinit var launcher: ActivityResultLauncher<I>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ContractHelper.logger.logStr("ContractFragment($contract) registerForActivityResult")
        launcher = registerForActivityResult(contract) {
            ContractHelper.logger.logStr("ContractFragment($contract) callback $it")
            callback?.onResult(it)
            callback = null
        }
    }

    fun launch(input: I?, callback: ContractHelper.OnResultListener<O>) {
        ContractHelper.logger.logStr("ContractFragment($contract) launch $input")
        this.callback = callback
        kotlin.runCatching { launcher.launch(input) }
    }
}
