package com.munch1182.p1

import android.content.Intent
import android.os.Bundle
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.munch1182.lib.base.launchIO
import com.munch1182.lib.base.startActivity
import com.munch1182.lib.base.statusHeight
import com.munch1182.lib.helper.result.intent
import com.munch1182.lib.helper.result.onData
import com.munch1182.p1.base.BaseActivity
import com.munch1182.p1.base.DataHelper
import com.munch1182.p1.base.Key
import com.munch1182.p1.base.str
import com.munch1182.p1.measure.MeasureHelper
import com.munch1182.p1.ui.ClickButton
import com.munch1182.p1.ui.PageTheme
import com.munch1182.p1.ui.RvPage
import com.munch1182.p1.ui.noApplyWindowPadding
import com.munch1182.p1.ui.setContentWithScroll
import com.munch1182.p1.ui.theme.PagePaddingModifier
import com.munch1182.p1.views.AboutActivity
import com.munch1182.p1.views.AudioActivity
import com.munch1182.p1.views.DialogActivity
import com.munch1182.p1.views.LanguageActivity
import com.munch1182.p1.views.ResultActivity
import com.munch1182.p1.views.ScanActivity
import com.munch1182.p1.views.SettingActivity
import com.munch1182.lib.base.str as strRes

class MainActivity : BaseActivity() {

    private val names: Array<Pair<Int, () -> Unit>> by lazy {
        arrayOf(
            R.string.permission_about to { startActivity<ResultActivity>() },
            R.string.dialog_about to { startActivity<DialogActivity>() },
            R.string.scan_about to { startActivity<ScanActivity>() },
            R.string.audio_about to { startActivity<AudioActivity>() },
            R.string.language_about to { intent(LanguageActivity::class.java).onData { recreateIfLangNeed(it) } },
            R.string.setting to { SettingActivity.startNames(names.map { it.first }) },
            R.string.about to { startActivity<AboutActivity>() },
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        MeasureHelper.measureNow("MainActivity app created")
        installSplashScreen()
        super.onCreate(savedInstanceState)
        MeasureHelper.measureNow("MainActivity installSplashScreen")
        setContentWithScroll(PagePaddingModifier.noApplyWindowPadding()) { Views(names) }
        MeasureHelper.measureNow("MainActivity setContent")

        lifecycleScope.launchIO {
            val name = DataHelper.get(Key.KEY_LAUNCHER, "")?.takeIf { it.isNotEmpty() }
            name?.let { n -> names.firstOrNull { strRes(it.first) == n }?.second?.invoke() }
        }
    }

    override fun onResume() {
        super.onResume()
        MeasureHelper.measureEnd()
    }

    @Composable
    private fun Views(names: Array<Pair<Int, () -> Unit>>) {
        with(LocalDensity.current) { Spacer(Modifier.height(statusHeight().toDp())) }
        LazyColumn { items(names) { ClickButton(str(it.first), onClick = it.second) } }
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
        PageTheme { RvPage(PagePaddingModifier) { Views(names) } }
    }
}

