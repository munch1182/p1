package com.munch1182.p1

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
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
import com.munch1182.lib.base.versionCodeCompat
import com.munch1182.lib.base.versionName
import com.munch1182.lib.helper.result.JudgeHelper.IntentCanLaunchDialogProvider
import com.munch1182.lib.helper.result.asAllowDenyDialog
import com.munch1182.lib.helper.result.intent
import com.munch1182.lib.helper.result.judge
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
import com.munch1182.p1.views.RecordActivity
import com.munch1182.p1.views.ResultActivity
import com.munch1182.p1.views.ServerActivity
import com.munch1182.p1.views.TaskActivity
import com.munch1182.p1.views.libview.ViewActivity

class MainActivity : BaseActivity() {

    private val log = log()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentWithBase { Click() }
        startActivity<RecordActivity>()
    }

    override fun recreate() {
        super.recreate()
        log.logStr("recreate()")
    }

    @Composable
    private fun Click() {
        JumpButton("权限相关", clazz = ResultActivity::class)
        JumpButton("录音相关", clazz = RecordActivity::class)
        JumpButton("服务相关", clazz = ServerActivity::class)
        JumpButton("弹窗相关", clazz = DialogActivity::class)
        JumpButton("任务队列", clazz = TaskActivity::class)
        JumpButton("View相关", clazz = ViewActivity::class)
        ClickButton("语言切换") {
            intent(Intent(this, LanguageActivity::class.java)).request { recreateIfLangNeed(it.data) }
        }
        ClickButton("开发者选项") { toDeveloperSettings() }
        JumpButton("设置界面", intent = Intent(Settings.ACTION_SETTINGS))
        JumpButton("关于界面", intent = Intent(Settings.ACTION_DEVICE_INFO_SETTINGS))
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
        val devIntent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (isInDeveloperMode()) intent(devIntent).request {}
        judge { isInDeveloperMode() }
            .intent(Intent(Settings.ACTION_DEVICE_INFO_SETTINGS))
            .dialogWhen(developerDialog())
            .request {
                if (it) intent(devIntent).request {}
            }
    }

    private fun developerDialog(): IntentCanLaunchDialogProvider {
        return IntentCanLaunchDialogProvider { ctx, state ->
            if (state.isAfter) {
                null
            } else {
                AlertDialog.Builder(ctx)
                    .setTitle("打开开发者选项")
                    .setMessage("请连续点击版本号直到系统提示开发者模式已打开")
                    .setPositiveButton("前往") { _, _ -> }
                    .setNegativeButton("取消") { _, _ -> }
                    .create().asAllowDenyDialog()
            }
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun GreetingPreview() {
        P1Theme { Click() }
    }
}

