package com.munch1182.lib.result

import android.app.Activity
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope

class InvisibleFragment : Fragment() {

    companion object {
        const val TAG = "com.munch1182.lib.result.InvisibleFragment"
    }

    private var resultCallback: ((Boolean, Intent?) -> Unit)? = null
    private var permissionCallback: (() -> Unit)? = null

    private val permission =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            permissionCallback?.invoke()
            permissionCallback = null
        }
    private val result =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            resultCallback?.invoke(it.resultCode == Activity.RESULT_OK, it.data)
            resultCallback = null
        }

    internal val scope: LifecycleCoroutineScope
        get() = requireActivity().lifecycleScope

    //当未申请过权限时，此方法返回false
    //当第一次拒绝时，此方法返回true
    //当被永久拒绝时，此方法返回false
    //所以无法在申请权限前就判断是否被永久拒绝
    private fun showRationale(permission: String) =
        shouldShowRequestPermissionRationale(permission)


    internal fun startActivity4Result(intent: Intent, resultCallback: (Boolean, Intent?) -> Unit) {
        this.resultCallback = resultCallback
        result.launch(intent)
    }

    internal fun startPermission(permissions: Array<String>, permissionCallback: () -> Unit) {
        this.permissionCallback = permissionCallback
        permission.launch(permissions)
    }
}