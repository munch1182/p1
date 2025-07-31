package com.munch1182.p1.views

import android.os.Build
import android.os.Bundle
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalInspectionMode
import com.munch1182.lib.base.navigationHeight
import com.munch1182.lib.base.screen
import com.munch1182.lib.base.screenDisplay
import com.munch1182.lib.base.statusHeight
import com.munch1182.lib.base.versionCodeCompat
import com.munch1182.lib.base.versionName
import com.munch1182.p1.base.BaseActivity
import com.munch1182.p1.ui.setContentWithRv

class AboutActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentWithRv { Views() }
    }

    @Composable
    private fun Views() {
        MainInfo()
    }

    @Composable
    private fun MainInfo() {
        Column(horizontalAlignment = Alignment.Start) {
            val isPreMode = LocalInspectionMode.current
            Text(if (isPreMode) "0.1.0(1)" else "$versionName($versionCodeCompat)")
            Text(if (isPreMode) "1080(1080) x 2400(80 + 2320)" else screenStr())
            Text("CURR SDK: ${Build.VERSION.SDK_INT}")
        }
    }

    private fun screenStr(): String {
        val sc = screen()
        val sd = screenDisplay()
        val equalsHeight = sc.height() == (sd.heightPixels + statusHeight())
        val navHeight = if (equalsHeight) 0 else navigationHeight()
        return "${sc.width()}(${sd.widthPixels}) x ${sc.height()}(${statusHeight()} + ${sd.heightPixels} + $navHeight)"
    }
}