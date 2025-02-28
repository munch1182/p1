package com.munch1182.p1

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.munch1182.lib.clearScreenOn
import com.munch1182.lib.findActivity
import com.munch1182.lib.isDeveloperMode
import com.munch1182.lib.keepScreenOn
import com.munch1182.lib.toast
import com.munch1182.p1.ui.theme.P1Theme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentWithBase { Click() }

        startActivity(Intent(this, SlideMenuViewActivity::class.java))
    }
}

@Composable
fun Click() {
    val ctx = LocalContext.current
    var keepFlag by remember { mutableStateOf(false) }
    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        JumpButton("悬浮窗", Intent(ctx, FloatWindowActivity::class.java))
        JumpButton("悬浮窗XML", Intent(ctx, FloatWindowViewActivity::class.java))
        JumpButton("滑动菜单", Intent(ctx, SlideMenuViewActivity::class.java))
        ClickButton("开发者选项界面", toDeveloperSettings(ctx))
        JumpButton("设置界面", Intent(Settings.ACTION_SETTINGS))
        JumpButton("关于", Intent(Settings.ACTION_DEVICE_INFO_SETTINGS))
        //  JumpButton("测试", Intent("com.tencent.mm.plugin.appbrand.ui.AppBrandUI00"))
        ClickButton(if (keepFlag) "关闭屏幕常亮" else "保持屏幕常亮", {
            val act = ctx.findActivity() ?: return@ClickButton
            keepFlag = !keepFlag
            if (keepFlag) act.keepScreenOn() else act.clearScreenOn()
        })
    }
}


fun toDeveloperSettings(ctx: Context): () -> Unit {
    if (ctx.isDeveloperMode()) {
        return { ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)) }
    } else {
        // todo 去请求关于界面打开开发者模式并返回跳转
        return { ctx.toast("开发者模式未开启") }
    }
}


@Composable
fun ClickButton(text: String, onClick: () -> Unit) {
    Button(onClick, modifier = Modifier.fillMaxWidth()) {
        Text(text)
    }
}

@Composable
fun JumpButton(text: String, intent: Intent) {
    val ctx = LocalContext.current
    ClickButton(text) { ctx.startActivity(intent) }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    P1Theme {
        Click()
    }
}
