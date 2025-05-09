package com.munch1182.p1.views

import android.os.Bundle
import androidx.activity.ComponentDialog
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.window.Dialog
import com.munch1182.lib.base.withColor
import com.munch1182.lib.helper.currAct
import com.munch1182.lib.helper.dialog.container
import com.munch1182.lib.helper.dialog.onResult
import com.munch1182.p1.base.BaseActivity
import com.munch1182.p1.base.DialogHelper
import com.munch1182.p1.base.show
import com.munch1182.p1.base.toast
import com.munch1182.p1.ui.ClickButton
import com.munch1182.p1.ui.Split
import com.munch1182.p1.ui.setContentWithRv

// https://developer.android.com/develop/ui/views/components/dialogs?hl=zh-cn
class DialogActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentWithRv { View() }
    }

    @Composable
    private fun View() {
        val show = remember { mutableStateOf(false) }
        ClickButton("DialogFragment") { showDialogFragment() }
        ClickButton("ComposeDialog") { show.value = true }
        if (show.value) ComposeDialog(show)

        Split()

        ClickButton("YesNoLaterDialog") { showYesNoLaterDialog() }

        Split()

        ClickButton("Message") {
            DialogHelper.newMessage(DialogHelper.Message(msg = "这是一条必须要处理的消息，请确认或者拒绝", ok = "确认", cancel = "拒绝".withColor("red"))).onResult { toast("选择了 $it") }.show()
        }

        ClickButton("Progress") {
            DialogHelper.newProgress(DialogHelper.Progress(msg = "正在加载中，请稍候...")).onResult { if (it) toast("已取消") }.show()
        }

        ClickButton("Bottom") {
            DialogHelper.newBottomChose(arrayOf("选项1", "选项2", "选项3")) { toast("选择了 ${it + 1}") }.show()
        }
    }

    private fun showDialogFragment() = newAlertDialog().container().show()
    private fun newAlertDialog() = AlertDialog.Builder(currAct).setTitle("标题").setMessage("这是一条内容，用于在弹窗中显示。").setNegativeButton(android.R.string.cancel) { _, _ -> }.setPositiveButton(android.R.string.ok) { _, _ -> }.create()
    private fun showYesNoLaterDialog() = ComponentDialog(currAct).apply { setContentView(ComposeView(currAct).apply { setContent { DialogContent() } }) }.show()

    @Composable
    private fun DialogContent() {
        Column {
            Text("标题")
            Text("这是一条内容，用于在弹窗中显示。")
            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                Text("取消")
                Text("确定")
            }
        }
    }

    @Composable
    private fun ComposeDialog(show: MutableState<Boolean>) {
        Dialog({ show.value = false }) { DialogContent() }
    }
}