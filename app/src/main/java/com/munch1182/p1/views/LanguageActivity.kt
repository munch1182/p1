package com.munch1182.p1.views

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import com.munch1182.lib.base.statusHeight
import com.munch1182.p1.R
import com.munch1182.p1.base.BaseActivity
import com.munch1182.p1.base.LanguageHelper
import com.munch1182.p1.base.str
import com.munch1182.p1.ui.CheckBoxLabel
import com.munch1182.p1.ui.ClickButton
import com.munch1182.p1.ui.SplitV
import com.munch1182.p1.ui.noApplyWindowPadding
import com.munch1182.p1.ui.setContentWithScroll
import com.munch1182.p1.ui.theme.PagePaddingModifier
import java.util.Locale

class LanguageActivity : BaseActivity() {


    companion object {
        private const val KEY_NEED_RECREATE = "LanguageActivity_KEY_NEED_RECREATE"
        fun isNeedRecreate(intent: Intent?): Boolean {
            return intent?.getBooleanExtra(KEY_NEED_RECREATE, false) == true
        }

        private fun dispatchRecreate2UpdateLang(act: Activity, update: Boolean) {
            act.setResult(RESULT_OK, Intent().apply { putExtra(KEY_NEED_RECREATE, update) })
        }
    }

    private var updateLang = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentWithScroll(PagePaddingModifier.noApplyWindowPadding()) { View() }
        savedInstanceState?.let { updateLang = it.getBoolean(KEY_NEED_RECREATE) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // 此页面重启后传递值
        outState.putBoolean(KEY_NEED_RECREATE, updateLang)
    }

    @Composable
    private fun View() {
        val locs by remember { mutableStateOf(arrayOf(LanguageHelper.currSystemLocale(), Locale.CHINA, Locale.TRADITIONAL_CHINESE, Locale.ENGLISH, Locale("ar"))) }
        var selectIndex by remember { mutableIntStateOf(locs.indexOfLast { it.toLanguageTag() == LanguageHelper.curr.toLanguageTag() }) }

        // 因为本页面切换抖动，所以采用这种方式
        with(LocalDensity.current) { Spacer(Modifier.height(statusHeight().toDp())) }
        Text("${str(R.string.curr_lang)}: ${LanguageHelper.currSystemLocale()}")

        SplitV()

        LazyColumn {
            items(locs.size) { index ->
                val lang = locs[index]
                CheckBoxLabel(if (index == 0) "系统" else lang.displayName, selectIndex == index) {
                    if (it) selectIndex = index
                }
            }
        }
        ClickButton("切换语言") {
            updateLang = true
            LanguageHelper.updateLocale(locs[selectIndex].toLanguageTag())
            recreate()
        }
    }

    override fun finish() {
        dispatchRecreate2UpdateLang(this, updateLang)
        super.finish()
    }
}