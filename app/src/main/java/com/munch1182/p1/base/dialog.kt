package com.munch1182.p1.base

import android.content.Context
import android.content.DialogInterface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.ComponentDialog
import androidx.annotation.StyleRes
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.munch1182.lib.AppHelper
import com.munch1182.lib.base.DialogViewCtxProvider
import com.munch1182.lib.base.findAct
import com.munch1182.lib.base.getFiled
import com.munch1182.lib.base.launchIO
import com.munch1182.lib.base.lpW
import com.munch1182.lib.helper.currAct
import com.munch1182.lib.helper.currAsFM
import com.munch1182.lib.helper.dialog.AllowDenyDialogContainer
import com.munch1182.lib.helper.dialog.ResultDialog
import com.munch1182.lib.helper.dialog.asAllowDenyDialog
import com.munch1182.lib.helper.result.JudgeHelper
import com.munch1182.lib.helper.result.PermissionHelper
import com.munch1182.p1.ui.ClickButton
import com.munch1182.p1.ui.ComposeView
import com.munch1182.p1.ui.Split
import com.munch1182.p1.ui.theme.PagePadding
import com.munch1182.p1.ui.theme.PagePaddingHalf
import kotlinx.coroutines.delay

object DialogHelper {

    private fun newDialog(msg: Message, ctx: Context) = MyWindowDialog.Builder(ctx).setTitle(msg.title).setMessage(msg.msg).setPositiveButton(msg.ok) { _, _ -> }.setNegativeButton(msg.cancel) { _, _ -> }.setCancelable(msg.isCancelable).create()

    fun newMessage(
        title: String = "提示", msg: String, ok: String = AppHelper.getString(android.R.string.ok), cancel: String = AppHelper.getString(android.R.string.cancel), isCancelable: Boolean = true, ctx: Context = currAct
    ): AllowDenyDialogContainer = newDialog(Message(title, msg, ok, cancel, isCancelable), ctx).asAllowDenyDialog()

    fun newProgress(
        msg: CharSequence, isCancelable: Boolean = true, cancel: String = AppHelper.getString(android.R.string.cancel), ctx: Context = currAct
    ): ProgressDialog {
        return ProgressDialog.new(Progress(msg, isCancelable, cancel), ctx)
    }

    fun newBottom(vp: DialogViewCtxProvider): DFBottom {
        return DFBottom().inject(vp)
    }

    fun newBottom(content: @Composable (DialogInterface?) -> Unit): DFBottom {
        return DFBottom().inject { it, dialog -> ComposeView(it) { content(dialog) } }
    }

    fun newBottom(names: Array<String>, select: (Int) -> Unit): DFBottom {
        var df: DFBottom? = null
        df = newBottom {
            LazyColumn(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = PagePadding)
            ) {
                items(names.size) {
                    Text(
                        names[it],
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = {
                                select(it)
                                df?.dismiss()
                            })
                            .padding(vertical = PagePaddingHalf)
                    )
                }
            }
        }
        return df
    }

    fun <VB : ViewBinding> newBottom(inflater: (LayoutInflater, ViewGroup?, Boolean) -> VB, onViewCreated: VB.() -> Unit): DFBottom {
        return DFBottom().inject(inflater, onViewCreated)
    }


    private object ViewImpl {
        fun newProgress(ctx: Context, progress: Progress, showCancel: MutableState<Boolean>, onClick: () -> Unit): View {
            return ComposeView(ctx) {
                Column(
                    modifier = Modifier
                        .wrapContentSize()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White) // 圆角 间距 背景色 有先后顺序
                        .padding(PagePadding), horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                    Text(progress.msg.toString(), modifier = Modifier.padding(32.dp, 8.dp))
                    Split()
                    if (showCancel.value) {
                        Split()
                        ClickButton(progress.cancel, onClick = onClick)
                    }
                }
            }
        }
    }

    // 至少用于包装数据，并不对外暴露
    private class Message(val title: String, val msg: String, val ok: String = AppHelper.getString(android.R.string.ok), val cancel: String = AppHelper.getString(android.R.string.cancel), val isCancelable: Boolean = false)
    internal class Progress(val msg: CharSequence, val isCancelable: Boolean = false, val cancel: String = AppHelper.getString(android.R.string.cancel))

    private class MyWindowDialog(ctx: Context, @StyleRes resId: Int = 0) : AlertDialog(ctx, resId) {

        private var backgroundDrawable: Drawable? = null

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            window?.setLayout(lpW, lpW)
            backgroundDrawable?.let { window?.setBackgroundDrawable(it) }
        }

        fun setBackground(drawable: Drawable?) {
            this.backgroundDrawable = drawable
        }

        class Builder(private val ctx: Context, @StyleRes private val resId: Int = 0) : AlertDialog.Builder(ctx, resId) {

            private var backgroundDrawable: Drawable? = null

            fun background(draw: Drawable? = "#00000000".toColorInt().toDrawable()): Builder {
                backgroundDrawable = draw
                return this
            }

            // 需要兼容性测试
            override fun create(): AlertDialog {

                val dialog = MyWindowDialog(ctx, resId)

                dialog.setBackground(backgroundDrawable)

                val p = this.getFiled<Any>("P", AlertDialog.Builder::class.java)
                val alert = dialog.getFiled<Any>("mAlert", AlertDialog::class.java)

                p.javaClass.getMethod("apply", alert.javaClass).invoke(p, alert)

                val clazz = p.javaClass
                val cancelable = p.getFiled<Boolean>("mCancelable", clazz)
                dialog.setCancelable(cancelable)
                if (cancelable) {
                    dialog.setCanceledOnTouchOutside(true)
                }
                dialog.setOnCancelListener(p.getFiled("mOnCancelListener", clazz))
                dialog.setOnDismissListener(p.getFiled("mOnDismissListener", clazz))
                val filed = p.getFiled<DialogInterface.OnKeyListener?>("mOnKeyListener", clazz)
                if (filed != null) {
                    dialog.setOnKeyListener(filed)
                }
                return dialog
            }
        }
    }

    open class CancelDialog(private val dialog: ComponentDialog? = null) : DialogFragment(), ResultDialog<Boolean> {
        private var isCanceled = false
        override val result: Boolean get() = isCanceled
        override val lifecycle: Lifecycle get() = dialog?.lifecycle ?: throw IllegalStateException("dialog is null")

        override fun onCreateDialog(savedInstanceState: Bundle?) = dialog ?: throw IllegalStateException("dialog is null")

        fun canceledByHandle() {
            isCanceled = true
        }

        override fun show() {
            show(currAsFM.supportFragmentManager, null)
        }
    }

    class ProgressDialog private constructor(private val progress: Progress, private val ctx: Context) : CancelDialog() {
        companion object {
            internal fun new(progress: Progress, ctx: Context): ProgressDialog {
                return ProgressDialog(progress, ctx)
            }
        }

        private val showCancel = mutableStateOf(false)
        override val result: Boolean = true
        private val dialog by lazy { createDialog() }

        override val lifecycle get() = dialog.lifecycle
        override fun onCreateDialog(savedInstanceState: Bundle?) = dialog

        private fun createDialog(): ComponentDialog {
            return MyWindowDialog.Builder(ctx).background().setCancelable(progress.isCancelable)
                .setView(ViewImpl.newProgress(ctx, progress, showCancel) {
                    dismiss()
                }).create()
        }

        fun showCancel(show: Boolean = true) {
            showCancel.value = show
        }

        fun showCancelDelay(delay: Long = 5000, show: Boolean = true): ProgressDialog {
            val act = ctx.findAct() as? ComponentActivity?
            act?.lifecycleScope?.launchIO {
                delay(delay)
                showCancel(show)
            }
            return this
        }
    }

    /**
     * 使用DF无法使用OnResult
     */
    open class DFBottom : BottomSheetDialogFragment(), ResultDialog<Boolean> {
        private var dProvider: DialogViewCtxProvider? = null
        fun inject(provider: DialogViewCtxProvider?) = this.apply { this.dProvider = provider }

        private var vbInflater: ((LayoutInflater, ViewGroup?, Boolean) -> ViewBinding)? = null
        private var bind: ViewBinding? = null
        private var onViewCreated: ((ViewBinding) -> Unit)? = null

        @Suppress("UNCHECKED_CAST")
        fun <VB : ViewBinding> inject(inflater: (LayoutInflater, ViewGroup?, Boolean) -> VB, onViewCreated: (VB) -> Unit): DFBottom {
            this.vbInflater = inflater
            this.onViewCreated = { onViewCreated.invoke(it as VB) }
            return this
        }

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            val bind = bind ?: vbInflater?.invoke(inflater, container, false)?.apply { bind = this }
            if (bind != null) return bind.root
            return (container?.context ?: context)?.let { dProvider?.onCreateView(it, dialog) } ?: super.onCreateView(inflater, container, savedInstanceState)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            bind?.let { onViewCreated?.invoke(it) }
        }

        override val result: Boolean get() = false

        override fun show() {
            show(currAsFM.supportFragmentManager, null)
        }
    }
}

fun PermissionHelper.Input.dialogPermission(name: String, use: String) = dialogWhen { ctx, state, _ ->
    val (msg, sure) = when (state) {
        PermissionHelper.State.Before -> return@dialogWhen null
        PermissionHelper.State.Denied -> "正在请求${name}权限, 请允许该权限用于${use}." to "授权"
        PermissionHelper.State.DeniedForever -> "${name}权限被拒绝，请前往设置界面手动开启该权限，否则${use}功能无法使用。" to "前往"
    }
    DialogHelper.newMessage("权限请求", msg, sure)
}

fun JudgeHelper.Dialog.intentDialog(before: String?, after: String?) = this.dialogWhen { _, state ->
    val msg = when (state) {
        JudgeHelper.State.Before -> before
        JudgeHelper.State.After -> after
    } ?: return@dialogWhen null
    DialogHelper.newMessage(msg, "前往授权", "前往")
}

fun JudgeHelper.Dialog.intentBlueScanDialog() = this.dialogWhen { _, _ ->
    DialogHelper.newMessage("权限申请", "蓝牙扫描附近设备，需要定位功能及权限，请打开定位功能", "去打开")
}

fun PermissionHelper.Input.permissionBlueScanDialog() = this.dialogWhen { _, state, _ ->
    val (msg, ok) = when (state) {
        PermissionHelper.State.DeniedForever -> "定位权限已被拒绝, 需要前往设置页面手动开启定位权限, 否则无法扫描到蓝牙设备。" to "去开启"
        else -> "蓝牙扫描附近设备，需要定位权限，请允许定位权限" to "允许"
    }
    DialogHelper.newMessage("蓝牙扫描权限", msg, ok)
}.manualIntent()