package com.munch1182.p1.base

import android.content.Context
import android.content.DialogInterface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.viewbinding.ViewBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.munch1182.lib.AppHelper
import com.munch1182.lib.base.DialogViewCtxProvider
import com.munch1182.lib.base.getFiled
import com.munch1182.lib.base.lpW
import com.munch1182.lib.base.str
import com.munch1182.lib.helper.currAct
import com.munch1182.lib.helper.currAsFM
import com.munch1182.lib.helper.dialog.AllowDenyDialogContainer
import com.munch1182.lib.helper.dialog.ResultDialog
import com.munch1182.lib.helper.dialog.asAllowDenyDialog
import com.munch1182.lib.helper.result.JudgeHelper
import com.munch1182.lib.helper.result.PermissionHelper
import com.munch1182.p1.ui.Split
import com.munch1182.p1.ui.theme.PagePadding


fun PermissionHelper.Input.permissionIntentDialogWithName(name: String) = this.dialogWhen { _, state, _ ->
    if (state.isDeniedForever) DialogHelper.newMessage("请前往设置界面手动允许${name}权限") else null
}.manualIntent()

fun JudgeHelper.Dialog.intentDialog(before: String?, after: String?) = this.dialogWhen { _, state ->
    val msg = when (state) {
        JudgeHelper.State.Before -> before
        JudgeHelper.State.After -> after
    } ?: return@dialogWhen null
    DialogHelper.newMessage(msg, "前往授权", "前往")
}

fun JudgeHelper.Dialog.intentBlueScanDialog() = this.dialogWhen { _, _ ->
    DialogHelper.newMessage("蓝牙扫描附近设备，需要定位功能及权限，请打开定位功能", "权限申请", "去打开")
}

fun PermissionHelper.Input.permissionDialog(name: String, toUse: String, noBefore: Boolean = true, noDenied: Boolean = false) = this.dialogWhen { _, state, _ ->
    val (msg, ok) = when (state) {
        PermissionHelper.State.Before -> if (noBefore) return@dialogWhen null else "正在申请${name}相关权限, 用于${toUse}。" to "允许"
        PermissionHelper.State.Denied -> if (noDenied) return@dialogWhen null else "正在申请${name}相关权限, 用于${toUse}。" to "允许"
        PermissionHelper.State.DeniedForever -> "该权限已被拒绝, 需要前往设置页面手动开启${name}权限, 否则该功能无法使用。" to "去开启"
    }
    DialogHelper.newMessage(msg, "权限申请", ok)
}

fun PermissionHelper.Input.permissionBlueScanDialog() = this.dialogWhen { _, state, _ ->
    val (msg, ok) = when (state) {
        PermissionHelper.State.DeniedForever -> "定位权限已被拒绝, 需要前往设置页面手动开启定位权限, 否则无法扫描到蓝牙设备。" to "去开启"
        else -> "蓝牙扫描附近设备，需要定位权限，请允许定位权限" to "允许"
    }
    DialogHelper.newMessage(msg, "蓝牙扫描权限", ok)
}.manualIntent()

object DialogHelper {

    private fun newDialog(msg: Message, ctx: Context = currAct) = MyWindowDialog.Builder(ctx).setTitle(msg.title).setMessage(msg.msg).setPositiveButton(msg.ok) { _, _ -> }.setNegativeButton(msg.cancel) { _, _ -> }.setCancelable(msg.isCancelable).create()

    fun newMessage(msg: Message, ctx: Context = currAct) = newDialog(msg, ctx).asAllowDenyDialog()

    fun newMessage(msg: CharSequence, title: CharSequence = str(android.R.string.dialog_alert_title), ok: CharSequence = str(android.R.string.ok), cancel: CharSequence = AppHelper.getString(android.R.string.cancel), isCancelable: Boolean = true): AllowDenyDialogContainer {
        return newMessage(Message(msg, title, ok, cancel, isCancelable))
    }

    fun newProgress(progress: Progress, ctx: Context = currAct): CancelDialog {
        var cd: CancelDialog? = null
        val dialog = MyWindowDialog.Builder(ctx).background()
            .setView(
                ViewImpl.newProgress(ctx, progress) {
                    cd?.canceledByHandle()
                    cd?.dismiss()
                }).create()
        cd = CancelDialog(dialog)
        return cd
    }

    fun newProgress(msg: CharSequence, isCancel: Boolean = true, cancel: CharSequence = str(android.R.string.cancel)): CancelDialog {
        return newProgress(Progress(msg, isCancel, cancel))
    }

    fun newBottomChose(items: Array<String>, onChose: ((Int) -> Unit)? = null): DialogFragment {
        return newBottom { ctx, d ->
            ViewImpl.newChose(ctx, items) {
                d?.cancel()
                onChose?.invoke(it)
            }
        }
    }

    fun newBottom(vp: DialogViewCtxProvider): DFBottom {
        return DFBottom().inject(vp)
    }

    fun <VB : ViewBinding> newBottom(inflater: (LayoutInflater, ViewGroup?, Boolean) -> VB, onViewCreated: VB.() -> Unit): DFBottom {
        return DFBottom().inject(inflater, onViewCreated)
    }

    data class Message(
        val msg: CharSequence, val title: CharSequence = str(android.R.string.dialog_alert_title), val ok: CharSequence = str(android.R.string.ok), val cancel: CharSequence = AppHelper.getString(android.R.string.cancel), val isCancelable: Boolean = true
    )

    data class Progress(val msg: CharSequence, val isCancel: Boolean = true, val cancel: CharSequence = str(android.R.string.cancel))

    private object ViewImpl {
        fun newProgress(ctx: Context, progress: Progress, onCancel: (() -> Unit)? = null): View {
            return ComposeView(ctx).apply {
                setContent {
                    Column(
                        modifier = Modifier
                            .wrapContentSize()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White) // 圆角 间距 背景色 有先后顺序
                            .padding(PagePadding), horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Split()
                        AndroidView({ ctx -> TextView(ctx).apply { text = progress.msg } })
                        if (progress.isCancel) {
                            Split()
                            AndroidView({ ctx ->
                                Button(ctx, null, android.R.style.TextAppearance_Material_Button).apply {
                                    text = progress.cancel
                                    setOnClickListener { onCancel?.invoke() }
                                }
                            })
                        }
                    }
                }
            }
        }

        fun newChose(ctx: Context, items: Array<String>, onChose: ((Int) -> Unit)? = null): View {
            return ComposeView(ctx).apply {
                setContent {
                    Column {
                        Split()
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            itemsIndexed(items) { i, item ->
                                Text(
                                    text = item, modifier = Modifier
                                        .clickable { onChose?.invoke(i) }
                                        .padding(PagePadding)
                                        .fillMaxWidth()
                                        .align(Alignment.CenterHorizontally)
                                        .wrapContentSize(Alignment.Center))
                            }
                        }
                        Split()
                        Text(
                            str(android.R.string.cancel), modifier = Modifier
                                .clickable { onChose?.invoke(items.size) }
                                .padding(PagePadding)
                                .fillMaxWidth()
                                .align(Alignment.CenterHorizontally)
                                .wrapContentSize(Alignment.Center))
                        Split()
                    }
                }
            }

        }
    }

    class DFBottom : BottomSheetDialogFragment(), ResultDialog<Boolean> {
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

    class CancelDialog(private val dialog: ComponentDialog) : DialogFragment(), ResultDialog<Boolean> {
        private var isCanceled = false
        override val result: Boolean get() = isCanceled
        override val lifecycle: Lifecycle get() = dialog.lifecycle

        override fun onCreateDialog(savedInstanceState: Bundle?) = dialog

        fun canceledByHandle() {
            isCanceled = true
        }

        override fun show() {
            show(currAsFM.supportFragmentManager, null)
        }
    }

    class MyWindowDialog(ctx: Context, @StyleRes resId: Int = 0) : AlertDialog(ctx, resId) {

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
}