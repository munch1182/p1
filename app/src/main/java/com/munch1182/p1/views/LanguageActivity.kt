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
import com.munch1182.lib.base.findActivity
import com.munch1182.p1.R
import com.munch1182.p1.base.LanguageHelper
import com.munch1182.p1.base.str
import com.munch1182.p1.ui.CheckBoxWithLabel
import com.munch1182.p1.ui.ClickButton
import com.munch1182.p1.ui.setContentWithNoScroll
import java.util.Locale

// https://developer.android.google.cn/guide/topics/resources/app-languages.html?hl=zh-cn#impl-overview
class LanguageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentWithNoScroll { View() }
    }

    @Composable
    fun View() {
        val locs by remember { mutableStateOf(arrayOf(Locale.CHINA, Locale.ENGLISH, Locale("ar"))) }
        var selectIndex by remember { mutableIntStateOf(locs.indexOfFirst { it.toLanguageTag() == LanguageHelper.currLocale().toLanguageTag() }) }
        val curr = LocalContext.current

        Text("${str(R.string.current_language)}: ${LanguageHelper.currLocale()}")
        ClickButton(str(R.string.switch_language)) {
            curr.findActivity()?.finish()
            LanguageHelper.updateLocale(locs[selectIndex].toLanguageTag())
        }
        LazyColumn {
            items(locs.size) { index ->
                val lang = locs[index]
                CheckBoxWithLabel(lang.displayLanguage, selectIndex == index) {
                    if (it) selectIndex = index
                }
            }
        }
    }
}