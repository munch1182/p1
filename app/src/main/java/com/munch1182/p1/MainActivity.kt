package com.munch1182.p1

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.munch1182.lib.base.isInDeveloperMode
import com.munch1182.lib.base.navigationHeight
import com.munch1182.lib.base.screen
import com.munch1182.lib.base.screenDisplay
import com.munch1182.lib.base.startActivity
import com.munch1182.lib.base.statusHeight
import com.munch1182.lib.base.toast
import com.munch1182.lib.base.versionCodeCompat
import com.munch1182.lib.base.versionName
import com.munch1182.p1.base.BaseActivity
import com.munch1182.p1.base.LanguageHelper
import com.munch1182.p1.base.curr
import com.munch1182.p1.base.str
import com.munch1182.p1.ui.ClickButton
import com.munch1182.p1.ui.JumpButton
import com.munch1182.p1.ui.setContentWithBase
import com.munch1182.p1.ui.theme.P1Theme
import com.munch1182.p1.ui.theme.PagePadding
import com.munch1182.p1.views.DialogActivity
import com.munch1182.p1.views.LanguageActivity
import com.munch1182.p1.views.ResultActivity
import com.munch1182.p1.views.TaskActivity

class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentWithBase { Click() }
        startActivity<ResultActivity>()
    }

    @Composable
    fun Click() {
        JumpButton("权限相关", clazz = ResultActivity::class)
        JumpButton("弹窗相关", clazz = DialogActivity::class)
        JumpButton("任务队列", clazz = TaskActivity::class)
        JumpButton("语言切换", clazz = LanguageActivity::class)
        ClickButton("开发者选项") { toDeveloperSettings() }
        JumpButton("设置界面", intent = Intent(Settings.ACTION_SETTINGS))
        JumpButton("关于节目", intent = Intent(Settings.ACTION_DEVICE_INFO_SETTINGS))
        Column(
            modifier = Modifier.padding(top = PagePadding),
            horizontalAlignment = Alignment.Start
        ) {
            Text("$versionName($versionCodeCompat)")
            Text(screenStr())
            Text("CURR SDK: ${Build.VERSION.SDK_INT}")
            Text("${str(R.string.curr_lang)}: ${LanguageHelper.currLocale()}")
        }
    }

    private fun screenStr(): String {
        val sc = screen()
        val sd = screenDisplay()
        val equalsHeight = sc.height() == (sd.heightPixels + statusHeight())
        val navHeight = if (equalsHeight) 0 else navigationHeight()
        return "${sc.width()}(${sd.widthPixels}) x ${sc.height()}(${statusHeight()} + ${sd.heightPixels} + $navHeight)"
    }

    private fun toDeveloperSettings() {
        if (isInDeveloperMode()) {
            curr.startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
        } else {
            toast("开发者模式未开启")
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun GreetingPreview() {
        P1Theme { Click() }
    }
}

