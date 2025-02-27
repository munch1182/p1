package com.munch1182.p1

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
        FWManager.hideAll()
    }

    private fun Context.showFloatWindow() {
        val ctx = this.applicationContext
        val minView = FrameLayout(ctx).apply {
            addView(Button(ctx).apply {
                text = "最小化"
            })
            setPadding(
                8.dp.value.toInt(),
                64.dp.value.toInt(),
                8.dp.value.toInt(),
                64.dp.value.toInt()
            )
            tag = "minView"
            setOnClickListener {

            }
        }
        val floatWindow = FrameLayout(ctx).apply {
            addView(TextView(ctx).apply {
                text = "悬浮窗"
            })
            setPadding(48.dp.value.toInt())
            setBackgroundColor(Color.parseColor("#ffffff"))
            tag = "floatWindow"
        }
        val card = CardView(ctx).apply { addView(floatWindow) }

        if (FWManager.isDestroy()) {
            FWManager.create(card).setEdgeMoveListener {

            }
        }
        FWManager.show()
    }

    private fun setupFloatWindow(type: Int, setGravity: MutableState<Int>? = null) {
        FWManager.update {
            when (type) {
                0 -> {
                    val v = FWManager.findView() ?: return@update false
                    val loc = IntArray(2)
                    v.getLocationOnScreen(loc)

                    x = loc[0] * -1
                    y = loc[1] * -1
                }

                1 -> {
                    x = 0
                    y = 0
                }

                2 -> {
                    val newGravity = when (gravity) {
                        Gravity.CENTER -> Gravity.TOP or Gravity.START
                        Gravity.TOP or Gravity.START -> Gravity.CENTER
                        else -> Gravity.CENTER
                    }
                    gravity = newGravity
                    setGravity?.value = newGravity

                    x = 0
                    y = 0
                }
            }
            return@update true
        }
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
        val gravity = remember { mutableIntStateOf(Gravity.CENTER) }
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
            Item("显示悬浮窗(${showString(gravity)})(与0,0有关)") { setupFloatWindow(2, gravity) }
            Item("更新悬浮窗(0,0)") { setupFloatWindow(1) }
            Item("更新悬浮窗(左上角)") { setupFloatWindow(0) }
            Spacer(Modifier.height(32.dp))
            Item("销毁悬浮窗") { FWManager.destroyAll() }
        }
    }


    @Composable
    fun Item(text: String, onClick: Context.() -> Unit) {
        val ctx = LocalContext.current
        Button(onClick = { onClick(ctx) }, modifier = Modifier.fillMaxWidth()) {
            Text(text)
        }
    }

    private fun showString(gravity: MutableState<Int>): String {
        return when (gravity.value) {
            Gravity.CENTER -> "CENTER"
            Gravity.TOP or Gravity.START -> "TOP_LEFT"
            else -> "CENTER"
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
