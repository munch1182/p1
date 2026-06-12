package com.munch1182.core.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.munch1182.core.ui.currAsFragmentActivityOrThrow
import com.munch1182.lib.android.IResultPrompt
import com.munch1182.lib.android.IResultPromptControl
import java.util.UUID


/**
 * 在fragment中传入ui
 */
internal class CommonBottomDialogFragment<T> : BottomSheetDialogFragment(), IResultPrompt<T>, IResultPromptControl<T> {

    private var onShow: (() -> Unit)? = null
    private var onDismiss: ((T?) -> Unit)? = null
    private val key by lazy { arguments?.getString(ARG_CONTENT_KEY) }
    private val registry by lazy { ViewModelProvider(requireActivity())[ContentRegistryViewModel::class.java] }

    override val current: T? // 不缓存在此fragment中而是放在vm中
        get() = key?.let {
            @Suppress("UNCHECKED_CAST") //
            registry.getResult(it) as? T? //
        }

    override fun set(value: T) {
        key?.let { registry.setResult(it, value) }
    }

    override fun setCancellable(cancellable: Boolean) {
        isCancelable = cancellable
        dialog?.setCancelable(cancellable)
        dialog?.setCanceledOnTouchOutside(cancellable)
        (dialog as? BottomSheetDialog)?.behavior?.isHideable = cancellable
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (dialog as? BottomSheetDialog)?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
        dialog?.window?.apply {
            addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            setBackgroundDrawable(null)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        registry // 初始化
        val key = key ?: return super.onCreateView(inflater, container, savedInstanceState)
        val content = registry.getContent(key) ?: return super.onCreateView(inflater, container, savedInstanceState)
        return ComposeView(requireContext()).apply {
            setContent {
                CompositionLocalProvider(LocalResultControl provides this@CommonBottomDialogFragment) { content() }
            }
        }
    }

    override fun show() {
        if (isAdded) return // 避免重复添加
        val fm = currAsFragmentActivityOrThrow.supportFragmentManager
        fm.beginTransaction().add(this, FRAG_TAG).commitAllowingStateLoss()
    }

    override fun onShow(onShow: () -> Unit): CommonBottomDialogFragment<T> {
        this.onShow = onShow
        return this
    }

    override fun onDismiss(onDismiss: (T?) -> Unit): CommonBottomDialogFragment<T> {
        this.onDismiss = onDismiss
        return this
    }

    // 在对话框真正关闭时回调结果
    override fun dismiss() {
        onDismiss?.invoke(current)   // 假设结果不为空，根据需求处理 null
        super.dismiss()
    }

    override fun dismissAllowingStateLoss() {
        onDismiss?.invoke(current!!)
        super.dismissAllowingStateLoss()
    }

    companion object {
        /***
         * 用于在compose中获取当前dialog的实例
         */
        internal val LocalResultControl = compositionLocalOf<IResultPromptControl<*>> {
            error("No ResultControl provided")
        }
        private const val ARG_CONTENT_KEY = "content_key"
        private const val FRAG_TAG = "com.munch1182.core.ui.dialog.CommonBottomDialogFragment"

        fun <R> newInstance(key: String): CommonBottomDialogFragment<R> {
            return CommonBottomDialogFragment<R>().apply {
                arguments = Bundle().apply { putString(ARG_CONTENT_KEY, key) }
            }
        }
    }
}

/**
 * 实际上(在activity生命周期内)保存所有content函数, 用于在页面重启后重新获取ui
 */
internal class ContentRegistryViewModel : ViewModel() {
    private val contentMap = mutableMapOf<String, @Composable () -> Unit>()
    private val resultMap = mutableMapOf<String, Any?>()

    fun register(content: @Composable () -> Unit): String {
        val key = UUID.randomUUID().toString()
        contentMap[key] = content
        return key
    }

    fun getContent(key: String): (@Composable () -> Unit)? = contentMap[key]

    fun setResult(key: String, value: Any?) {
        resultMap[key] = value
    }

    fun getResult(key: String): Any? = resultMap[key]

    fun unregister(key: String) {
        contentMap.remove(key)
        resultMap.remove(key)
    }

    override fun onCleared() {
        contentMap.clear()
        resultMap.clear()
    }
}