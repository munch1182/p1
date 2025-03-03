package com.munch1182.p1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.munch1182.lib.ClipboardHelper
import com.munch1182.lib.ClipboardHelper.cbm
import com.munch1182.lib.appDetailsPage
import com.munch1182.lib.findActivity
import com.munch1182.lib.simpleDateStr
import com.munch1182.p1.ui.theme.P1Theme

class ClipboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentWithBase { Clipboard(it) }
    }
}

@Composable
fun Clipboard(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current.findActivity() ?: return
    var clipData by remember { mutableStateOf("") }
    var hasData by remember { mutableStateOf(cbm?.hasPrimaryClip() ?: false) }
    Column(modifier = modifier.padding(16.dp)) {
        Button({
            ctx.startActivity(appDetailsPage)
        }) { Text("权限页面跳转") }
        Spacer(Modifier.height(16.dp))
        Button({
            hasData = cbm?.hasPrimaryClip() ?: false
            clipData = ClipboardHelper.copyFrom2Str() ?: ""
        }) { Text("检查数据") }
        Button({
            ClipboardHelper.putTo(simpleDateStr("yyyy-MM-dd HH:mm:ss"))
            clipData = ClipboardHelper.copyFrom2Str() ?: ""
        }) { Text("写入剪切板") }
        Button({
            ClipboardHelper.putTo(simpleDateStr("yyyy-MM-dd HH:mm:ss"), true)
            clipData = ClipboardHelper.copyFrom2Str() ?: ""
        }) { Text("写入剪切板(隐私)") }

        Button({
            ClipboardHelper.clear()
            clipData = ClipboardHelper.copyFrom2Str() ?: ""
        }) { Text("清空剪切板") }

        Text(if (hasData) clipData else "无数据或者无权限", modifier = Modifier.padding(16.dp))
    }
}

@Preview
@Composable
fun ClipboardPreview() {
    P1Theme { Clipboard() }
}