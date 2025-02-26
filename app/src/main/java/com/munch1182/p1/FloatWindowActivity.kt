package com.munch1182.p1

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.cardview.widget.CardView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.setPadding
import com.munch1182.lib.floatwindow.FWManager
import com.munch1182.lib.keepScreenOn
import com.munch1182.p1.ui.theme.P1Theme

class FloatWindowActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        keepScreenOn()
        setContentWithBase { FloatWindow() }
    }


    private fun hideFloatWindow() {
        FWManager.hide()
    }

    private fun Context.showFloatWindow() {
        val textView = TextView(this.applicationContext)
        textView.text = "悬浮窗"

        val fl = FrameLayout(this.applicationContext).apply {
            layoutParams =
                FrameLayout.LayoutParams(200.dp.value.toInt(), 200.dp.value.toInt()).apply {
                    gravity = Gravity.CENTER
                }
            addView(textView)
            setPadding(16.dp.value.toInt())
            setBackgroundColor(Color.parseColor("#ffffff"))
        }
        val card = CardView(this.applicationContext).apply {
            addView(fl)
        }


        FWManager.create(card).show()
    }

    private fun Context.requestPermission() {
        // 仍需要注册SYSTEM_ALERT_WINDOW权限
        startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${packageName}")
            )
        )
    }


    @Composable
    fun FloatWindow() {
        val ctx = LocalContext.current
        var permissionState by remember { mutableStateOf(FWManager.canDrawOverlays(ctx)) }
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("悬浮窗权限状态：${permissionState}")
            Item("检查悬浮窗权限") { permissionState = FWManager.canDrawOverlays(this) }
            Item("申请悬浮窗权限") { requestPermission() }
            Spacer(Modifier.height(32.dp))
            Item("显示悬浮窗") { showFloatWindow() }
            Item("隐藏悬浮窗") { hideFloatWindow() }
        }
    }


    @Composable
    fun Item(text: String, onClick: Context.() -> Unit) {
        val ctx = LocalContext.current
        Button(onClick = { onClick(ctx) }, modifier = Modifier.fillMaxWidth()) {
            Text(text)
        }
    }

    @Preview
    @Composable
    fun FloatWindowPreview() {
        P1Theme {
            FloatWindow()
        }
    }
}
