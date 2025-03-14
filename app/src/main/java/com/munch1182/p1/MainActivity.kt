package com.munch1182.p1

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.munch1182.lib.base.clearScreenOn
import com.munch1182.lib.base.findActivity
import com.munch1182.lib.base.isDeveloperMode
import com.munch1182.lib.base.keepScreenOn
import com.munch1182.lib.base.navigationHeight
import com.munch1182.lib.base.screen
import com.munch1182.lib.base.screenDisplay
import com.munch1182.lib.base.startActivity
import com.munch1182.lib.base.statusHeight
import com.munch1182.lib.base.toast
import com.munch1182.lib.base.versionCodeCompat
import com.munch1182.lib.base.versionName
import com.munch1182.p1.ui.theme.P1Theme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentWithBase { Click() }
        startActivity<RecyclerViewActivity>()
    }
}

@Composable
fun Click() {
    val ctx = LocalContext.current
    val act = ctx.findActivity()
    var keepFlag by remember { mutableStateOf(false) }
    JumpButton("分贝相关", Intent(ctx, SoundMeterActivity::class.java))
    JumpButton("滑动列表", Intent(ctx, RecyclerViewActivity::class.java))
    JumpButton("权限相关", Intent(ctx, ResultActivity::class.java))
    JumpButton("相机相关", Intent(ctx, CameraActivity::class.java))
    JumpButton("定位相关", Intent(ctx, LocationActivity::class.java))
    JumpButton("dialog相关", Intent(ctx, DialogActivity::class.java))
    JumpButton("剪切板相关", Intent(ctx, ClipboardActivity::class.java))
    JumpButton("测试相关", Intent(ctx, TestBtnActivity::class.java))
    ClickButton("开发者选项界面", toDeveloperSettings(ctx))
    JumpButton("设置界面", Intent(Settings.ACTION_SETTINGS))
    JumpButton("关于界面", Intent(Settings.ACTION_DEVICE_INFO_SETTINGS))
    ClickButton(if (keepFlag) "关闭屏幕常亮" else "保持屏幕常亮", {
        keepFlag = !keepFlag
        if (keepFlag) act?.keepScreenOn() else act?.clearScreenOn()
    })
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(screenStr())
        Text("CURR SDK: ${Build.VERSION.SDK_INT}")
        Text("$versionName($versionCodeCompat)")
    }
}

private fun screenStr(): String {
    val sc = screen()
    val sd = screenDisplay()
    val equalsHeight = sc.height() == (sd.heightPixels + statusHeight())
    val navHeight = if (equalsHeight) 0 else navigationHeight()
    return "${sc.width()}(${sd.widthPixels}) x ${sc.height()}(${statusHeight()} + ${sd.heightPixels} + $navHeight)"
}


fun toDeveloperSettings(ctx: Context): () -> Unit {
    if (isDeveloperMode()) {
        return { ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)) }
    } else {
        // todo 去请求关于界面打开开发者模式并返回跳转
        return { toast("开发者模式未开启") }
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
    P1Theme { Click() }
}
