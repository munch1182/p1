package com.munch1182.p1

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
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
import com.munch1182.lib.clearScreenOn
import com.munch1182.lib.findActivity
import com.munch1182.lib.isDeveloperMode
import com.munch1182.lib.keepScreenOn
import com.munch1182.lib.navigationHeight
import com.munch1182.lib.screen
import com.munch1182.lib.screenDisplay
import com.munch1182.lib.statusHeight
import com.munch1182.lib.toast
import com.munch1182.lib.versionCodeCompat
import com.munch1182.lib.versionName
import com.munch1182.p1.ui.theme.P1Theme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        keepScreenOn()
        setContent {
            P1Theme {
                Scaffold(modifier = Modifier.fillMaxWidth()) { innerPadding ->
                    Click(Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun Click(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val act = ctx.findActivity()
    var keepFlag by remember { mutableStateOf(false) }
    Column(modifier = modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
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
            Text("${versionName}($versionCodeCompat)")
        }
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
    P1Theme {
        Click()
    }
}
