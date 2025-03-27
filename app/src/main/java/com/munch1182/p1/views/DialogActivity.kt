package com.munch1182.p1.views

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.window.Dialog
import com.munch1182.p1.base.curr
import com.munch1182.p1.ui.ClickButton
import com.munch1182.p1.ui.Split
import com.munch1182.p1.ui.setContentWithBase

class DialogActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentWithBase { View() }
    }

    @Composable
    fun View() {
        val showComposeDialog = remember { mutableStateOf(false) }
        ClickButton("系统弹窗") { showSysDialog() }
        ClickButton("系统Compose") { showComposeDialog.value = true }
        if (showComposeDialog.value) ComposeDialog(showComposeDialog)
        Split()


    }

    @Composable
    private fun ComposeDialog(showComposeDialog: MutableState<Boolean>) {
        Dialog({ showComposeDialog.value = false }) { Text("这是一条内容，用于在弹窗中显示。") }
    }

    private fun showSysDialog() {
        androidx.appcompat.app.AlertDialog.Builder(curr).setTitle("标题").setMessage("这是一条内容，用于在弹窗中显示。").setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }.setPositiveButton(android.R.string.ok) { dialog, _ -> dialog.dismiss() }.show()
    }
}