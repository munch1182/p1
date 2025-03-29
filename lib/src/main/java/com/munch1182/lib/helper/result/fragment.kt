package com.munch1182.lib.helper.result

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.munch1182.lib.base.Any2StrFmt
import com.munch1182.lib.base.OnResultListener
import com.munch1182.lib.base.newLog


/**
 * fragment的构造传参会在页面重建(如旋转时)失去数据
 * 解决办法：
 *  1. 锁定方向
 *  2. 旋转等不触发重建
 *  否则会因为重建时fragment会被调用无参构造导致抛出异常
 */
class ContractFragment<I, O>(private val contract: ActivityResultContract<I, O>) : Fragment() {

    companion object {
        private const val TAG = "com.munch1182.lib.helper.result.ContractFragment"
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

    private val log = ContractHelper.log.newLog("ContractFragment")
    private var callback: OnResultListener<O>? = null
    private lateinit var launcher: ActivityResultLauncher<I>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        log.logStr("$contract registerFroResult")
        launcher = registerForActivityResult(contract) {
            log.logStr("result($it)")
            if (callback == null) log.logStr("but callback is null")
            callback?.onResult(it)
            callback = null
        }
    }

    fun launch(input: I?, callback: OnResultListener<O>) {
        log.logStr("$contract launch $input")
        this.callback = callback
        kotlin.runCatching { launcher.launch(input) }
    }
}

class PermissionIntentFragment : Fragment() {

    companion object {
        private const val TAG = "com.munch1182.lib.helper.result.PermissionIntentFragment"
        fun get(fm: FragmentManager): PermissionIntentFragment {
            return fm.findFragmentByTag(TAG) as? PermissionIntentFragment ?: PermissionIntentFragment().apply {
                fm.beginTransaction().add(this, TAG).commitNowAllowingStateLoss()
            }
        }
    }


    private val log = ContractHelper.log.newLog("PermissionIntentFragment")
    private var permissionCallback: OnResultListener<Map<String, Boolean>>? = null
    private val permission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        log.logStr("result($it)")
        permissionCallback?.onResult(it)
        permissionCallback = null
    }
    private var resultCallback: OnResultListener<ActivityResult>? = null
    private val result = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        log.logStr("result($it)")
        resultCallback?.onResult(it)
        resultCallback = null
    }

    init {
        log.logStr("registerFroPermission registerFroIntent")
    }

    fun launch(intent: Intent, callback: OnResultListener<ActivityResult>) {
        log.logStr("launch $intent")
        resultCallback = callback
        result.launch(intent)
    }

    fun launch(permissions: Array<String>, callback: OnResultListener<Map<String, Boolean>>) {
        log.logStr("launch ${Any2StrFmt.any2Str(permissions)}")
        permissionCallback = callback
        permission.launch(permissions)
    }
}