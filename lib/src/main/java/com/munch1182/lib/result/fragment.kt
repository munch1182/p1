package com.munch1182.lib.result

import android.annotation.SuppressLint
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
        @SuppressLint("UNUSED")
        const val TAG = "com.munch1182.lib.result.PermissionIntentFragment"

        internal fun get(fm: FragmentManager): Fragment {
            return fm.findFragmentByTag(TAG) ?: PermissionIntentFragment().apply {
                fm.beginTransaction().add(this, TAG).commitNowAllowingStateLoss()
            }
        }
    }

    private var permissionCallback: ContractHelper.OnResultListener<Map<String, Boolean>>? = null
    private var intentCallback: ContractHelper.OnResultListener<ActivityResult>? = null

    private val permission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        permissionCallback?.onResult(it)
        permissionCallback = null
    }
    private val intent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        intentCallback?.onResult(it)
        intentCallback = null
    }

    fun launchPermission(permissions: Array<String>?, listener: ContractHelper.OnResultListener<Map<String, Boolean>>) {
        this.permissionCallback = listener
        this.permission.launch(permissions)
    }

    fun launchIntent(intent: Intent?, listener: ContractHelper.OnResultListener<ActivityResult>) {
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
        const val TAG = "com.munch1182.lib.result.ContractFragment"
    }

    private var callback: ContractHelper.OnResultListener<O>? = null
    private lateinit var launcher: ActivityResultLauncher<I>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launcher = registerForActivityResult(contract) {
            callback?.onResult(it)
            callback = null
        }
    }

    fun launch(input: I?, callback: ContractHelper.OnResultListener<O>) {
        this.callback = callback
        kotlin.runCatching { launcher.launch(input) }
    }
}
