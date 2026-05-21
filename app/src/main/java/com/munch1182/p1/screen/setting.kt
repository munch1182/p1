package com.munch1182.p1.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.munch1182.core.common.toARGBColorStr
import com.munch1182.p1.R
import com.munch1182.p1.domain.DarkMode
import com.munch1182.p1.domain.LanguageType
import com.munch1182.p1.domain.LanguageVM
import com.munch1182.p1.domain.ThemeType
import com.munch1182.p1.domain.ThemeVM
import com.munch1182.p1.ui.AccordionLabelItem
import com.munch1182.p1.ui.Checkbox
import com.munch1182.p1.ui.PrimaryButton
import com.munch1182.p1.ui.ScrollPage
import com.munch1182.p1.ui.SplitH
import com.munch1182.p1.ui.SplitW
import com.munch1182.p1.ui.theme.Dimens
import com.munch1182.p1.ui.theme.colorRandom
import com.munch1182.p1.ui.theme.paddingPage
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph

/**
 * 设置界面
 */
@Destination<RootGraph>
@Composable
fun SettingScreen() {
    ScrollPage(applyPadding = false) {
        ThemeSetting()
        SplitH()
        LangSetting()
    }
}

@Composable
private fun LangSetting(langVM: LanguageVM = hiltViewModel()) {
    val currLangType by langVM.currLanguageType.collectAsStateWithLifecycle()

    val followSystem = stringResource(R.string.follow_system)

    val currLangStr by remember {
        derivedStateOf {
            when (currLangType) {
                is LanguageType.FollowSystem -> followSystem
                is LanguageType.Specific -> (currLangType as LanguageType.Specific).lang
            }
        }
    }

    // 简化使用, 只做示例使用
    val langs = listOf(stringResource(R.string.follow_system), "zh", "en", "ar")

    var isSelect by remember { mutableStateOf(false) }

    AccordionLabelItem(
        isSelect, modifier = Modifier.padding(end = Dimens.PaddingPage),
        onToggle = { isSelect = !isSelect },
        title = {
            Row(modifier = Modifier.paddingPage()) {
                Text(text = stringResource(R.string.curr_lang))
                Text(text = ": ")
                Text(text = currLangStr)
            }
        },
        content = {
            Column(modifier = Modifier.paddingPage()) {
                langs.forEachIndexed { idx, it ->
                    Checkbox(it, it == currLangStr) { select ->
                        if (select) {
                            if (idx == 0) {
                                langVM.reset()
                            } else {
                                langVM.switch(it)
                            }
                        }
                    }
                }
            }
        })
}

@Composable
private fun ThemeSetting(vm: ThemeVM = hiltViewModel()) {

    val curr by vm.currThemeData.collectAsStateWithLifecycle()
    val darkMode = listOf(DarkMode.FollowSystem, DarkMode.Light, DarkMode.Dark)

    var isSelect by remember { mutableStateOf(false) }

    AccordionLabelItem(isSelect, modifier = Modifier.padding(end = Dimens.PaddingPage), onToggle = { isSelect = !isSelect }, title = {
        Row(modifier = Modifier.paddingPage()) {
            Text(text = stringResource(R.string.curr_theme))
            Text(text = ": ")
            Text(text = curr.first.mode2Str())
            Text(text = "(${curr.second.mode2Str()})")
        }
    }, content = {
        Column(
            Modifier
                .paddingPage()
                .padding(top = 0.dp)
        ) {
            darkMode.forEach {
                Checkbox(it.mode2Str(), it == curr.second) { select ->
                    if (select) vm.switch(mode = it)
                }
            }
            Row {
                PrimaryButton(stringResource(R.string.reset_theme), onClick = vm::reset)
                SplitW()
                PrimaryButton(stringResource(R.string.random_mode)) {
                    val color = colorRandom()
                    vm.switch(type = ThemeType.Preset(color))
                }
            }
        }
    })
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