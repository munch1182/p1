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
import com.munch1182.lib.base.log
import com.munch1182.lib.base.navigationHeight
import com.munch1182.lib.base.screen
import com.munch1182.lib.base.screenDisplay
import com.munch1182.lib.base.startActivity
import com.munch1182.lib.base.statusHeight
import com.munch1182.lib.base.toast
import com.munch1182.lib.base.versionCodeCompat
import com.munch1182.lib.base.versionName
import com.munch1182.lib.helper.curr
import com.munch1182.lib.helper.result.intent
import com.munch1182.p1.base.BaseActivity
import com.munch1182.p1.base.LanguageHelper
import com.munch1182.p1.base.str
import com.munch1182.p1.ui.ClickButton
import com.munch1182.p1.ui.JumpButton
import com.munch1182.p1.ui.setContentWithBase
import com.munch1182.p1.ui.theme.P1Theme
import com.munch1182.p1.ui.theme.PagePadding
import com.munch1182.p1.views.DialogActivity
import com.munch1182.p1.views.LanguageActivity
import com.munch1182.p1.views.ResultActivity
import com.munch1182.p1.views.ServerActivity
import com.munch1182.p1.views.TaskActivity
import com.munch1182.p1.views.libview.SwapMenuLayoutActivity
import com.munch1182.p1.views.libview.ViewActivity

class MainActivity : BaseActivity() {

    private val log = log()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentWithBase { Click() }
        startActivity<SwapMenuLayoutActivity>()
    }

    override fun recreate() {
        super.recreate()
        log.logStr("recreate()")
    }

    @Composable
    private fun Click() {
        JumpButton("权限相关", clazz = ResultActivity::class)
        JumpButton("服务相关", clazz = ServerActivity::class)
        JumpButton("弹窗相关", clazz = DialogActivity::class)
        JumpButton("任务队列", clazz = TaskActivity::class)
        JumpButton("View相关", clazz = ViewActivity::class)
        ClickButton("语言切换") {
            intent(Intent(this, LanguageActivity::class.java)).request { recreateIfLangNeed(it.data) }
        }
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

    private fun recreateIfLangNeed(data: Intent?) {
        if (LanguageActivity.isNeedRecreate(this, data)) {
            log.logStr("lang update, need recreate")
            recreate()
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

