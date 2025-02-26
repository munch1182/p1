package com.munch1182.p1

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.compose.ui.unit.dp
import androidx.core.view.setPadding
import com.munch1182.lib.floatwindow.FWManager
import com.munch1182.lib.keepScreenOn

class FloatWindowViewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        keepScreenOn()
        setContentView(contentView())
    }

    @SuppressLint("SetTextI18n")
    private fun contentView(): View {
        val ctx = this
        val state = TextView(this).apply {
            text = "悬浮窗权限状态：${FWManager.canDrawOverlays(ctx)}"
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
        }
        val btn1 = btn("申请悬浮窗权限") { requestPermission() }
        val space = LinearLayout(this).apply {
            layoutParams =
                FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, 16.dp.value.toInt())
        }
        val btn2 = btn("显示悬浮窗") { showFloatWindow() }
        val btn3 = btn("隐藏悬浮窗") { hideFloatWindow() }

        return LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16.dp.value.toInt())
            addView(state)
            addView(btn1)
            addView(space)
            addView(btn2)
            addView(btn3)
        }
    }

    private fun btn(name: String, onClick: Context.() -> Unit): Button {
        return Button(this).apply {
            text = name
            setOnClickListener {
                onClick(this@FloatWindowViewActivity)
            }
        }
    }


    private fun hideFloatWindow() {
        FWManager.hide()
    }

    private fun Context.showFloatWindow() {
        val textView = TextView(this)
        textView.text = "悬浮窗"

        val fl = FrameLayout(this).apply {
            layoutParams =
                FrameLayout.LayoutParams(200.dp.value.toInt(), 200.dp.value.toInt()).apply {
                    gravity = Gravity.CENTER
                }
            addView(textView)
            setPadding(16.dp.value.toInt())
            setBackgroundColor(Color.parseColor("#ffffff"))
        }
        val card = CardView(this).apply {
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
}
