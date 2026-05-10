package com.munch1182.p1.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.munch1182.core.android.Log
import com.munch1182.core.common.toARGBColorStr
import com.munch1182.p1.R
import com.munch1182.p1.log.TAG_USER_EVENT
import com.munch1182.p1.ui.Checkbox
import com.munch1182.p1.ui.DarkMode
import com.munch1182.p1.ui.LangType
import com.munch1182.p1.ui.LanguageHelper
import com.munch1182.p1.ui.PrimaryButton
import com.munch1182.p1.ui.ScrollPage
import com.munch1182.p1.ui.SplitH
import com.munch1182.p1.ui.ThemeHelper
import com.munch1182.p1.ui.ThemeType
import com.munch1182.p1.ui.theme.colorRandom
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph

@Destination<RootGraph>
@Composable
fun SettingScreen() {
    ScrollPage {
        ThemeSetting()
        SplitH()
        LangSetting()
    }
}

@Composable
private fun LangSetting(langHelper: LanguageHelper = LanguageHelper) {
    val currLangType by langHelper.currLangType.collectAsStateWithLifecycle()
    val currLocale by langHelper.currLocale.collectAsStateWithLifecycle()

    val followSystem = stringResource(R.string.follow_system)

    val currLangStr by remember {
        derivedStateOf {
            when (currLangType) {
                is LangType.FollowSystem -> followSystem
                is LangType.Specific -> currLocale.toLanguageTag()
            }
        }
    }

    val langs = listOf(stringResource(R.string.follow_system), "zh", "en", "ar")

    Column {
        Row {
            Text(text = stringResource(R.string.curr_lang))
            Text(text = ": ")
            Text(text = currLangStr)
        }

        langs.forEachIndexed { idx, it ->
            Checkbox(it, it == currLangStr) { select ->
                if (select) {
                    Log.d(TAG_USER_EVENT, "更改语言：$it")
                    if (idx == 0) {
                        langHelper.reset()
                    } else {
                        langHelper.switch(it)
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeSetting(themeHelper: ThemeHelper = ThemeHelper) {
    val currThemeType by themeHelper.currThemeType.collectAsStateWithLifecycle()
    val currDarkMode by themeHelper.currDarkMode.collectAsStateWithLifecycle()


    val darkMode = listOf(DarkMode.FollowSystem, DarkMode.Light, DarkMode.Dark)


    Column {
        Row {
            Text(text = stringResource(R.string.curr_theme))
            Text(text = ": ")
            Text(text = currThemeType.mode2Str())
            Text(text = "(${currDarkMode.mode2Str()})")
        }
        darkMode.forEach {
            Checkbox(it.mode2Str(), it == currDarkMode) { select ->
                if (select) themeHelper.switch(mode = it)
            }
        }
        PrimaryButton(stringResource(R.string.reset_theme), onClick = themeHelper::reset)
        SplitH()
        PrimaryButton(stringResource(R.string.random_mode)) {
            val color = colorRandom()
            Log.d(TAG_USER_EVENT, "更改主题：${color}")
            themeHelper.switch(type = ThemeType.Preset(color))
        }
    }
}

@Composable
private fun ThemeType.mode2Str() = when (this) {
    ThemeType.FollowSystem -> stringResource(R.string.follow_system)
    is ThemeType.Preset -> this.id.toARGBColorStr()
}

@Composable
private fun DarkMode.mode2Str() = when (this) {
    DarkMode.FollowSystem -> stringResource(R.string.follow_system)
    DarkMode.Light -> stringResource(R.string.light_mode)
    DarkMode.Dark -> stringResource(R.string.dark_mode)
}