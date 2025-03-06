package com.munch1182.p1

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.munch1182.lib.base.findActivity
import com.munch1182.libview.dialog.DialogHelper
import com.munch1182.libview.dialog.DialogViewManager
import com.munch1182.libview.dialog.MessageDialog
import com.munch1182.libview.dialog.OnDialogCreateListener
import com.munch1182.p1.ui.ButtonDefault
import com.munch1182.p1.ui.Split
import com.munch1182.p1.ui.theme.P1Theme

class DialogActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentWithBase { Dialog() }
        DialogViewManager.setOnDialogCreateListener(object : OnDialogCreateListener {
            override fun onCreateMessage(context: Context): MessageDialog? {
                return null
            }
        })
    }
}

@Composable
fun Dialog() {
    val ctx = LocalContext.current
    ButtonDefault("系统默认弹窗") { showSysDialog(ctx) }
    Split()
    ButtonDefault("消息弹窗") {
        DialogHelper.newMessage("这是一条弹窗消息，需要你确认。").title("消息确认").show()
    }
    ButtonDefault("底部弹窗") {
        DialogHelper.newBottom(arrayOf("选项1", "选项2", "选项3")).canCancelNoChose().interceptDismiss { false }.onDismissListener { }.show()
    }
    ButtonDefault("顶部弹窗") {
        DialogHelper.newTop().show()
    }
    ButtonDefault("进度弹窗") {
        val dialog = DialogHelper.newProgress(0, 100)
        dialog.update(100)
        dialog.show()
    }
    ButtonDefault("提升弹窗") {
        DialogHelper.newPop().show()
    }
}

fun showSysDialog(ctx: Context) {
    val fm = (ctx.findActivity() as? FragmentActivity)?.supportFragmentManager ?: return
    DF().show(fm, "dialog")
}

class DF : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): android.app.Dialog {
        return AlertDialog.Builder(requireContext(), R.style.Theme_P1_Dialog).setTitle("title").setMessage("message").setPositiveButton(android.R.string.ok) { _, _ -> }.setNegativeButton(android.R.string.cancel) { _, _ -> }.create()
    }
}

@Preview
@Composable
fun DialogPreview() {
    P1Theme { Dialog() }
}