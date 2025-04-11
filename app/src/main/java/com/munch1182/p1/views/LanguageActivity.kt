package com.munch1182.p1.views

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.munch1182.lib.helper.currAct
import com.munch1182.p1.R
import com.munch1182.p1.base.BaseActivity
import com.munch1182.p1.base.LanguageHelper
import com.munch1182.p1.base.str
import com.munch1182.p1.ui.CheckBoxWithLabel
import com.munch1182.p1.ui.ClickButton
import com.munch1182.p1.ui.Split
import com.munch1182.p1.ui.setContentWithScroll
import java.util.Locale

// https://developer.android.google.cn/guide/topics/resources/app-languages.html?hl=zh-cn#impl-overview
class LanguageActivity : BaseActivity() {

    companion object {
        private const val KEY_UPDATE_LANG = "LanguageActivity_KEY_UPDATE_LANG"

        fun isNeedRecreate(intent: Intent?): Boolean {
            return intent?.getBooleanExtra(KEY_UPDATE_LANG, false) ?: false
        }

        private fun dispatchRecreate2UpdateLang(act: Activity) {
            act.setResult(Activity.RESULT_OK, Intent().putExtra(KEY_UPDATE_LANG, true))
            act.finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentWithScroll { View() }
    }

    @Composable
    private fun View() {
        val locs by remember { mutableStateOf(arrayOf(LanguageHelper.currSystemLocale(), Locale.CHINA, Locale.TRADITIONAL_CHINESE, Locale.ENGLISH, Locale("ar"))) }
        var selectIndex by remember { mutableIntStateOf(locs.indexOfLast { it.toLanguageTag() == LanguageHelper.curr.toLanguageTag() }) }

        Text("${str(R.string.curr_lang)}: ${LanguageHelper.currSystemLocale()}")

        Split()

        LazyColumn {
            items(locs.size) { index ->
                val lang = locs[index]
                CheckBoxWithLabel(if (index == 0) "系统" else lang.displayName, selectIndex == index) {
                    if (it) selectIndex = index
                }
            }
        }
        ClickButton("切换语言") {
            currAct.apply { dispatchRecreate2UpdateLang(this) }
            LanguageHelper.updateLocale(locs[selectIndex].toLanguageTag())
        }
    }
}