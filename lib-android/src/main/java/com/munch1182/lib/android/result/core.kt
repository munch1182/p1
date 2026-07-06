package com.munch1182.lib.android.result

import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import kotlin.coroutines.resume


/**
 * 使用[ActivityResultLauncher]启动并获取结果的无ui工具Fragment
 *
 * 适配旋转等调用页面重建的情形; 但内存回收仍会丢失状态;
 */
internal class ResultRequestFragment : Fragment() {

    companion object {
        private const val KEY_REQUEST_ID = "requestId"

        /**
         * 启动并获取结果
         */
        fun <I, O> start(
            act: FragmentActivity, contract: ActivityResultContract<I, O>, input: I, onResult: (O) -> Unit
        ) {
            val requestId = UUID.randomUUID().toString()
            // 获取同一个VM实例
            val vm = ViewModelProvider(act)[ResultRequestVM::class.java]
            @Suppress("UNCHECKED_CAST") // 因为是通过id一一对应的, 所以可以直接转换
            vm.put(
                requestId, RequestParams(
                    contract as ActivityResultContract<Any, Any>, input as Any, onResult as (Any?) -> Unit //也因为是一一对应的, 所以可以此次为可空的但0可以直接使用泛型
                )
            )

            val fragment = ResultRequestFragment()
            fragment.arguments = Bundle().apply {
                putString(KEY_REQUEST_ID, requestId)
            }
            act.supportFragmentManager.beginTransaction().add(fragment, "com.munch1182.lib.android.result.ResultRequestFragment").commitAllowingStateLoss()
        }
    }

    private val vm by lazy {
        ViewModelProvider(requireActivity())[ResultRequestVM::class.java]
    }

    private var requestId: String? = null
    private lateinit var launcher: ActivityResultLauncher<Any>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestId = arguments?.getString(KEY_REQUEST_ID)
        val param = requestId?.let { vm.get(it) }
        // 只有发起请求后附着的[FragmentActivity]跟随进程重建(发起请求后退到后台被系统回收), 才会为null(此时vm的map为空)
        if (param == null) {
            removeFragment()
            return
        }
        launcher = registerForActivityResult(param.contract) {
            param.onResult(it)
            removeFragment()
        }
        // // 仅在首次创建时发起请求，配置变更（如旋转）不再重复发起(仍会回调结果)
        if (savedInstanceState == null) {
            launcher.launch(param.input)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        requestId?.let(vm::remove)
    }

    private fun removeFragment() {
        parentFragmentManager.beginTransaction().remove(this).commitAllowingStateLoss()
    }
}

/**
 * 存储请求参数的 ViewModel，绑定到 [FragmentActivity] 的生命周期。
 * 因此配置变更（如旋转）时数据不丢失，Fragment 重建后可重新关联等待中的请求。
 * 请求完成后会主动从 map 中移除参数，不会造成内存泄漏。
 * 同时因为绑定的是Activity, 完成后此vm也不会移除自身;
 */
internal class ResultRequestVM : ViewModel() {
    private val request = mutableMapOf<String, RequestParams>()

    fun put(id: String, params: RequestParams) {
        request[id] = params
    }

    fun get(id: String): RequestParams? = request[id]
    fun remove(id: String) = request.remove(id)
}

internal data class RequestParams(
    val contract: ActivityResultContract<Any, Any>, val input: Any, val onResult: (Any?) -> Unit
)

/**
 * 使用启动一个[ActivityResultLauncher]并获取结果
 */
suspend fun <I, O> FragmentActivity.request(contract: ActivityResultContract<I, O>, input: I): O {
    return suspendCancellableCoroutine { continuation ->
        ResultRequestFragment.start(this, contract, input) {
            if (continuation.isActive) continuation.resume(it)
        }
    }
}