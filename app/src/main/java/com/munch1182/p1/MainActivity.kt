package com.munch1182.p1

import android.content.Intent
import android.os.Bundle
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.munch1182.lib.base.statusHeight
import com.munch1182.lib.helper.result.intent
import com.munch1182.lib.helper.result.onData
import com.munch1182.p1.base.BaseActivity
import com.munch1182.p1.base.str
import com.munch1182.p1.measure.MeasureHelper
import com.munch1182.p1.ui.ClickButton
import com.munch1182.p1.ui.JumpButton
import com.munch1182.p1.ui.PageTheme
import com.munch1182.p1.ui.RvPage
import com.munch1182.p1.ui.noApplyWindowPadding
import com.munch1182.p1.ui.setContentWithRv
import com.munch1182.p1.ui.theme.PagePaddingModifier
import com.munch1182.p1.views.AboutActivity
import com.munch1182.p1.views.DialogActivity
import com.munch1182.p1.views.LanguageActivity
import com.munch1182.p1.views.ResultActivity
import com.munch1182.p1.views.ScanActivity

class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        MeasureHelper.measureNow("MainActivity app created")
        installSplashScreen()
        super.onCreate(savedInstanceState)
        MeasureHelper.measureNow("MainActivity installSplashScreen")
        setContentWithRv(PagePaddingModifier.noApplyWindowPadding()) { Views() }
        MeasureHelper.measureNow("MainActivity setContent")
    }

    override fun onResume() {
        super.onResume()
        MeasureHelper.measureEnd()
    }

    @Composable
    private fun Views() {
        with(LocalDensity.current) { Spacer(Modifier.height(statusHeight().toDp())) }
        JumpButton(str(R.string.permission_about), clazz = ResultActivity::class)
        JumpButton(str(R.string.dialog_about), clazz = DialogActivity::class)
        JumpButton(str(R.string.scan_about), clazz = ScanActivity::class)
        ClickButton(str(R.string.language_about)) { intent(LanguageActivity::class.java).onData { recreateIfLangNeed(it) } }
        JumpButton(str(R.string.about), clazz = AboutActivity::class)
    }

    private fun recreateIfLangNeed(data: Intent) {
        if (LanguageActivity.isNeedRecreate(data)) {
            log.logStr("lang update, need recreate")
            recreate()
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun ClickPreview() {
        PageTheme { RvPage(PagePaddingModifier) { Views() } }
    }
}

