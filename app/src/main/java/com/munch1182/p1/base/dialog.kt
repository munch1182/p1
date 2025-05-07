package com.munch1182.p1.base

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.munch1182.lib.AppHelper
import com.munch1182.lib.base.ViewCtxProvider
import com.munch1182.lib.base.ViewInflaterProvider
import com.munch1182.lib.base.str
import com.munch1182.lib.helper.currAct
import com.munch1182.lib.helper.dialog.DialogContainer
import com.munch1182.lib.helper.dialog.asAllowDenyDialog
import com.munch1182.lib.helper.dialog.container
import com.munch1182.lib.helper.result.JudgeHelper
import com.munch1182.lib.helper.result.PermissionHelper
import com.munch1182.p1.ui.Split
import com.munch1182.p1.ui.theme.PagePadding

fun PermissionHelper.Input.permissionIntentDialogWithName(name: String) = this.dialogWhen { _, state, _ ->
    if (state.isDeniedForever) DialogHelper.newMessage(DialogHelper.Message("请前往设置界面手动允许${name}权限")) else null
}.manualIntent()

fun JudgeHelper.Dialog.intentDialog(before: String?, after: String?) = this.dialogWhen { _, state ->
    val msg = when (state) {
        JudgeHelper.State.Before -> before
        JudgeHelper.State.After -> after
    } ?: return@dialogWhen null
    DialogHelper.newMessage(DialogHelper.Message(msg, "前往授权", "前往"))
}

fun JudgeHelper.Dialog.intentBlueScanDialog() = this.dialogWhen { _, _ ->
    DialogHelper.newMessage(DialogHelper.Message("蓝牙扫描附近设备，需要定位功能及权限，请打开定位功能", "权限申请", "去打开"))
}

fun PermissionHelper.Input.permissionDialog(name: String, toUse: String, noBefore: Boolean = true, noDenied: Boolean = false) = this.dialogWhen { _, state, _ ->
    val (msg, ok) = when (state) {
        PermissionHelper.State.Before -> if (noBefore) return@dialogWhen null else "正在申请${name}相关权限, 用于${toUse}。" to "允许"
        PermissionHelper.State.Denied -> if (noDenied) return@dialogWhen null else "正在申请${name}相关权限, 用于${toUse}。" to "允许"
        PermissionHelper.State.DeniedForever -> "该权限已被拒绝, 需要前往设置页面手动开启${name}权限, 否则该功能无法使用。" to "去开启"
    }
    DialogHelper.newMessage(DialogHelper.Message(msg, "权限申请", ok))
}

fun PermissionHelper.Input.permissionBlueScanDialog() = this.dialogWhen { _, state, _ ->
    val (msg, ok) = when (state) {
        PermissionHelper.State.DeniedForever -> "定位权限已被拒绝, 需要前往设置页面手动开启定位权限, 否则无法扫描到蓝牙设备。" to "去开启"
        else -> "蓝牙扫描附近设备，需要定位权限，请允许定位权限" to "允许"
    }
    DialogHelper.newMessage(DialogHelper.Message(msg, "蓝牙扫描权限", ok))
}.manualIntent()

object DialogHelper {

    private fun newDialog(msg: Message, ctx: Context = currAct) = AlertDialog.Builder(ctx).setTitle(msg.title).setMessage(msg.msg).setPositiveButton(msg.ok) { _, _ -> }.setNegativeButton(msg.cancel) { _, _ -> }.setCancelable(msg.isCancel).create()

    fun newMessage(msg: Message, ctx: Context = currAct) = newDialog(msg, ctx).asAllowDenyDialog()

    fun newProgress(progress: Progress, ctx: Context = currAct, onCancel: (() -> Unit)? = null): DialogContainer {
        var dialog: AlertDialog? = null
        dialog = AlertDialog.Builder(ctx).setCancelable(false).setView(ViewImpl.newProgress(ctx, progress) {
            onCancel?.invoke()
            dialog?.cancel()
        }).create()
        return dialog.container()
    }

    fun newBottomChose(items: Array<String>, ctx: Context = currAct, onChose: ((Int) -> Unit)? = null): DialogFragment {
        val dialog = DFBottom()
        dialog.inject {
            ViewImpl.newChose(ctx, items) {
                onChose?.invoke(it)
                dialog.dismiss()
            }
        }
        return dialog
    }

    data class Message(
        val msg: CharSequence, val title: CharSequence = str(android.R.string.dialog_alert_title), val ok: CharSequence = str(android.R.string.ok), val cancel: CharSequence = AppHelper.getString(android.R.string.cancel), val isCancel: Boolean = true
    )

    data class Progress(val msg: CharSequence, val isCancel: Boolean = true, val cancel: CharSequence = str(android.R.string.cancel))

    private object ViewImpl {
        fun newProgress(ctx: Context, progress: Progress, onCancel: (() -> Unit)? = null): View {
            return ComposeView(ctx).apply {
                setContent {
                    Column(
                        modifier = Modifier
                            .wrapContentSize()
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
                                    text = item,
                                    modifier = Modifier
                                        .clickable { onChose?.invoke(i) }
                                        .padding(PagePadding)
                                        .fillMaxWidth()
                                        .align(Alignment.CenterHorizontally)
                                        .wrapContentSize(Alignment.Center)
                                )
                            }
                        }
                        Split()
                        Text(
                            str(android.R.string.cancel),
                            modifier = Modifier
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

    class DFBottom : BottomSheetDialogFragment() {
        private var dProvider: ViewCtxProvider? = null
        private var dInflater: ViewInflaterProvider? = null
        fun inject(provider: ViewCtxProvider?) = this.apply { this.dProvider = provider }
        fun injectLayout(provider: ViewInflaterProvider?) = this.apply { this.dInflater = provider }

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            return (container?.context ?: context)?.let { dProvider?.onCreateView(it) } ?: dInflater?.onCreateView(inflater, container) ?: super.onCreateView(inflater, container, savedInstanceState)
        }
    }
}