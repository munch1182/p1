package com.munch1182.p1.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.munch1182.core.android.IResultDialog

/**
 * 提供简化一个提供的消息弹窗；
 *
 * 内部使用[DialogFragment]来避免旋转等重建时的内存泄露/状态丢失等问题;
 * 因此必须在有[FragmentManager]的环境下使用
 */
class CommonDialog(
    private val fm: FragmentManager, //
    private val lifecycleOwner: LifecycleOwner, //
    title: String, //
    msg: String, //
    cancelable: Boolean = true, //
    ok: String, //
    cancel: String, //
) : IResultDialog<Boolean> {

    constructor(
        act: FragmentActivity, //
        title: String, //
        msg: String, //
        cancelable: Boolean = true, //
        ok: String = act.getString(android.R.string.ok), //
        cancel: String = act.getString(android.R.string.cancel) //
    ) : this(act.supportFragmentManager, act, title, msg, cancelable, ok, cancel)

    constructor(
        fm: Fragment, //
        title: String, //
        msg: String, //
        cancelable: Boolean = true, //
        ok: String = fm.getString(android.R.string.ok), //
        cancel: String = fm.getString(android.R.string.cancel) //
    ) : this(fm.childFragmentManager, fm.viewLifecycleOwner, title, msg, cancelable, ok, cancel)

    private val impl = CommonDialogFragment.newInstance(title, msg, cancelable, ok, cancel)
    private var onShowCallback: (() -> Unit)? = null
    private var onDismissCallback: ((Boolean) -> Unit)? = null

    override fun show() {
        if (impl.isAdded) return
        fm.setFragmentResultListener(CommonDialogFragment.REQUIRE_KEY, lifecycleOwner) { _, result ->
            val result = result.getBoolean(CommonDialogFragment.KEY_RESULT)
            onDismissCallback?.invoke(result)
        }
        impl.dialog?.setOnShowListener { onShowCallback?.invoke() }
        impl.show(fm, "com.munch1182.p1.dialog.CommonDialog")
    }

    override fun dismiss() {
        impl.dismissAllowingStateLoss()
        fm.clearFragmentResultListener(CommonDialogFragment.REQUIRE_KEY)
    }

    override fun onShow(onShow: () -> Unit): IResultDialog<Boolean> {
        onShowCallback = onShow
        return this
    }

    override fun onDismiss(onDismiss: (Boolean) -> Unit): IResultDialog<Boolean> {
        onDismissCallback = onDismiss
        return this
    }

}

/**
 * 提供一个默认的[DialogFragment]
 *
 * @param CommonDialog
 *
 * - 注意： 此类必须不能是私有的，因为系统使用构造创建
 */
class CommonDialogFragment : DialogFragment() {
    companion object {

        private const val KEY_TITLE = "title"
        private const val KEY_MESSAGE = "message"
        private const val KEY_CANCELABLE = "cancelable"
        private const val KEY_POSITIVE = "positive"
        private const val KEY_NEGATIVE = "negative"

        /**
         * 在[setFragmentResult]中使用的requestKey, 用于获取回调
         */
        const val REQUIRE_KEY = "com.munch1182.p1.base.CommonFragment"

        /**
         * 在[setFragmentResult]回调的[Bundle]的key
         */
        const val KEY_RESULT = "result"

        fun newInstance(
            title: String, //
            msg: String, //
            cancelable: Boolean = true, //
            ok: String, //
            cancel: String //
        ): CommonDialogFragment {
            return CommonDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(KEY_TITLE, title)
                    putString(KEY_MESSAGE, msg)
                    putBoolean(KEY_CANCELABLE, cancelable)
                    putString(KEY_POSITIVE, ok)
                    putString(KEY_NEGATIVE, cancel)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isCancelable = arguments?.getBoolean(KEY_CANCELABLE) ?: true
    }

    private fun sendResult(result: Boolean) {
        setFragmentResult(REQUIRE_KEY, Bundle().apply {
            putBoolean(KEY_RESULT, result)
        })
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val args = requireArguments() //
        return MaterialAlertDialogBuilder(requireContext()) //
            .setTitle(args.getString(KEY_TITLE)) //
            .setMessage(args.getString(KEY_MESSAGE)) //
            .setPositiveButton(args.getString(KEY_POSITIVE)) { _, _ -> sendResult(true) } //
            .setNegativeButton(args.getString(KEY_NEGATIVE)) { _, _ -> sendResult(false) } //
            .create() //
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        sendResult(false)
    }
}
