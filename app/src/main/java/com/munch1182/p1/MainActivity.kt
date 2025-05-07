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
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.munch1182.lib.base.isInDeveloperMode
import com.munch1182.lib.base.log
import com.munch1182.lib.base.navigationHeight
import com.munch1182.lib.base.screen
import com.munch1182.lib.base.screenDisplay
import com.munch1182.lib.base.statusHeight
import com.munch1182.lib.base.versionCodeCompat
import com.munch1182.lib.base.versionName
import com.munch1182.lib.helper.dialog.asAllowDenyDialog
import com.munch1182.lib.helper.result.JudgeHelper.IntentCanLaunchDialogProvider
import com.munch1182.lib.helper.result.intent
import com.munch1182.lib.helper.result.judge
import com.munch1182.lib.helper.result.onData
import com.munch1182.lib.helper.result.onTrue
import com.munch1182.p1.base.BaseActivity
import com.munch1182.p1.base.LanguageHelper
import com.munch1182.p1.base.str
import com.munch1182.p1.ui.ClickButton
import com.munch1182.p1.ui.JumpButton
import com.munch1182.p1.ui.PageTheme
import com.munch1182.p1.ui.RvPage
import com.munch1182.p1.ui.setContentWithRv
import com.munch1182.p1.ui.theme.PagePadding
import com.munch1182.p1.views.AudioActivity
import com.munch1182.p1.views.BluetoothActivity
import com.munch1182.p1.views.DialogActivity
import com.munch1182.p1.views.LanguageActivity
import com.munch1182.p1.views.ResultActivity
import com.munch1182.p1.views.ServerActivity
import com.munch1182.p1.views.TaskActivity
import com.munch1182.p1.views.TestActivity
import com.munch1182.p1.views.libview.ViewActivity

class MainActivity : BaseActivity() {

    private val log = log()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentWithRv { Click() }
        //startActivity<RecordActivity>()
    }

    @Composable
    private fun Click() {
        JumpButton("权限相关", clazz = ResultActivity::class)
        JumpButton("蓝牙相关", clazz = BluetoothActivity::class)
        JumpButton("音频相关", clazz = AudioActivity::class)
        JumpButton("服务相关", clazz = ServerActivity::class)
        JumpButton("弹窗相关", clazz = DialogActivity::class)
        JumpButton("任务队列", clazz = TaskActivity::class)
        JumpButton("View相关", clazz = ViewActivity::class)
        ClickButton("语言切换") { intent(Intent(this, LanguageActivity::class.java)).onData { recreateIfLangNeed(it) } }
        JumpButton("测试页面", clazz = TestActivity::class)
        ClickButton("开发者选项") { toDeveloperSettings() }
        JumpButton("设置界面", intent = Intent(Settings.ACTION_SETTINGS))
        JumpButton("关于界面", intent = Intent(Settings.ACTION_DEVICE_INFO_SETTINGS))
        MainInfo()
    }

    @Composable
    private fun MainInfo() {
        Column(
            modifier = Modifier.padding(top = PagePadding), horizontalAlignment = Alignment.Start
        ) {
            val isPreMode = LocalInspectionMode.current
            Text(if (isPreMode) "0.1.0(1)" else "$versionName($versionCodeCompat)")
            Text(if (isPreMode) "1080(1080) x 2400(80 + 2320)" else screenStr())
            Text("CURR SDK: ${Build.VERSION.SDK_INT}")
            Text(if (isPreMode) "当前语言：zh" else "${str(R.string.curr_lang)}: ${LanguageHelper.curr}")
        }
    }

    private fun recreateIfLangNeed(data: Intent) {
        if (LanguageActivity.isNeedRecreate(data)) {
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
        judge { isInDeveloperMode() }.intent(Intent(Settings.ACTION_DEVICE_INFO_SETTINGS)).dialogWhen(developerDialog()).onTrue { intent(devIntent).request {} }
    }

    private fun developerDialog(): IntentCanLaunchDialogProvider {
        return IntentCanLaunchDialogProvider { ctx, state ->
            if (state.isAfter) {
                null
            } else {
                AlertDialog.Builder(ctx).setTitle("打开开发者选项").setMessage("请连续点击版本号直到系统提示开发者模式已打开").setPositiveButton("前往") { _, _ -> }.setNegativeButton("取消") { _, _ -> }.create().asAllowDenyDialog()
            }
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun ClickPreview() {
        PageTheme { RvPage { Click() } }
    }
}

