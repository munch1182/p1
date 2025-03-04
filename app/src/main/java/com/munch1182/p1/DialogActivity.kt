package com.munch1182.p1

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.munch1182.lib.findActivity
import com.munch1182.p1.ui.theme.P1Theme

class DialogActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentWithBase { Dialog() }
    }
}

@Composable
fun Dialog() {
    val ctx = LocalContext.current
    Button({ showSysDialog(ctx) }) { Text("系统默认弹窗") }
    Spacer(Modifier.height(16.dp))
    Button({
        AlertDialog.Builder(ctx, R.style.Theme_P1_Dialog)
            .setTitle("title")
            .setMessage("message")
            .setPositiveButton(android.R.string.ok) { _, _ -> }
            .setNegativeButton(android.R.string.cancel) { _, _ -> }
            .create()
    }) { Text("弹窗样式") }
}

fun showSysDialog(ctx: Context) {
    val fm = (ctx.findActivity() as? FragmentActivity)?.supportFragmentManager ?: return
    DF().show(fm, "dialog")
}

class DF : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): android.app.Dialog {
        return AlertDialog.Builder(requireContext(), R.style.Theme_P1_Dialog)
            .setTitle("title")
            .setMessage("message")
            .setPositiveButton(android.R.string.ok) { _, _ -> }
            .setNegativeButton(android.R.string.cancel) { _, _ -> }
            .create()
    }
}

@Preview
@Composable
fun DialogPreview() {
    P1Theme { Dialog() }
}