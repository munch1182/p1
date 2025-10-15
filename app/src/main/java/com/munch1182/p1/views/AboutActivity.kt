package com.munch1182.p1.views

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.munch1182.lib.base.isDeveloperMode
import com.munch1182.lib.base.navigationHeight
import com.munch1182.lib.base.screen
import com.munch1182.lib.base.screenDisplay
import com.munch1182.lib.base.statusHeight
import com.munch1182.lib.base.versionCodeCompat
import com.munch1182.lib.base.versionName
import com.munch1182.lib.helper.result.judge
import com.munch1182.p1.base.BaseActivity
import com.munch1182.p1.base.DialogHelper
import com.munch1182.p1.ui.ClickButton
import com.munch1182.p1.ui.setContentWithRv

class AboutActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentWithRv { Click() }
    }

    @Composable
    fun Click() {
        Column {
            ClickButton("开发者选项界面") { toDeveloperSettings() }
            ClickButton("设置界面") { startActivity(Intent(Settings.ACTION_SETTINGS)) }
            ClickButton("关于界面") { startActivity(Intent(Settings.ACTION_DEVICE_INFO_SETTINGS)) }
            Column(
                modifier = Modifier.Companion
                    .fillMaxWidth()
                    .padding(16.dp), horizontalAlignment = Alignment.Companion.Start
            ) {
                Text(screenStr())
                Text("CURR SDK: ${Build.VERSION.SDK_INT}")
                Text("${versionName}(${versionCodeCompat})")
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

    fun toDeveloperSettings() {
        judge({ isDeveloperMode() }, Intent(Settings.ACTION_DEVICE_INFO_SETTINGS))
            .onDialog { DialogHelper.newMessage("打开开发者模式", "前往设置界面打开开发者模式") }
            .request { startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)) }
    }

}