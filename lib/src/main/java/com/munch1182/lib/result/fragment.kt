package com.munch1182.lib.result

import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.fragment.app.Fragment

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
