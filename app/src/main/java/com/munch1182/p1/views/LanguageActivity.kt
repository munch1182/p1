package com.munch1182.p1.views

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.munch1182.lib.base.Loglog
import com.munch1182.lib.base.findActivity
import com.munch1182.p1.R
import com.munch1182.p1.base.LanguageHelper
import com.munch1182.p1.base.str
import com.munch1182.p1.ui.CheckBoxWithLabel
import com.munch1182.p1.ui.ClickButton
import com.munch1182.p1.ui.Split
import com.munch1182.p1.ui.setContentWithNoScroll
import java.util.Locale

// https://developer.android.google.cn/guide/topics/resources/app-languages.html?hl=zh-cn#impl-overview
class LanguageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentWithNoScroll { View() }

        Locale.TRADITIONAL_CHINESE.let {
            Loglog.log("${it.language} ${it.country}  ${it.displayLanguage}  ${it.displayName}  ${it.toLanguageTag()}")
        }
    }

    @Composable
    fun View() {
        val locs by remember { mutableStateOf(arrayOf(LanguageHelper.currLocale(), Locale.CHINA, Locale.TRADITIONAL_CHINESE, Locale.ENGLISH, Locale("ar"))) }
        var selectIndex by remember { mutableIntStateOf(locs.indexOfLast { it.toLanguageTag() == LanguageHelper.currLocale().toLanguageTag() }) }
        val curr = LocalContext.current

        Text("${str(R.string.curr_lang)}: ${LanguageHelper.currLocale()}")
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
            curr.findActivity()?.finish()
            LanguageHelper.updateLocale(locs[selectIndex].toLanguageTag())
        }
    }
}