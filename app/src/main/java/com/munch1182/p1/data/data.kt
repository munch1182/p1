package com.munch1182.p1.data

import com.munch1182.core.android.DataHelper
import com.munch1182.p1.ui.DarkMode
import com.munch1182.p1.ui.LangType
import com.munch1182.p1.ui.ThemeType

private const val NAME_FOLLOW_SYSTEM = "FollowSystem"

//region theme
private const val KEY_THEME_KIND = "theme_kind"
private const val KEY_THEME_PRESET_ID = "theme_preset_id"
private const val KEY_DARK_MODE = "dark_mode"

private const val NAME_PRESET = "Preset"

/**
 * 保存主题设置
 * @param type 主题类型，可以是跟随系统或预设主题
 * @param mode 深色模式设置
 */
fun saveTheme(type: ThemeType, mode: DarkMode) {
    when (type) {
        ThemeType.FollowSystem -> {
            DataHelper.put(KEY_THEME_KIND, NAME_FOLLOW_SYSTEM)
            DataHelper.remove(KEY_THEME_PRESET_ID) // 清理残留
        }

        is ThemeType.Preset -> {
            DataHelper.put(KEY_THEME_KIND, NAME_PRESET)
            DataHelper.put(KEY_THEME_PRESET_ID, type.id)
        }
    }
    DataHelper.put(KEY_DARK_MODE, mode.name)
}

/**
 * 获取主题设置
 */
fun getTheme(): Pair<ThemeType, DarkMode> {
    val kind = DataHelper.getString(KEY_THEME_KIND) ?: NAME_FOLLOW_SYSTEM
    val theme = when (kind) {
        NAME_PRESET -> DataHelper.getLong(KEY_THEME_PRESET_ID)?.let { ThemeType.Preset(it) }
        else -> null
    } ?: ThemeType.FollowSystem
    val darkMode = DataHelper.getString(KEY_DARK_MODE)
        ?.let { save -> DarkMode.entries.find { it.name == save } }
        ?: DarkMode.FollowSystem
    return theme to darkMode
}
//endregion

//region lang
private const val KEY_LANG_TYPE = "lang_type"
private const val KEY_LANG = "lang"

/**
 * 保存语言设置
 *
 * 只保存LangType， 具体的lang应该由app的语言偏好获取
 */
fun saveLang(langType: LangType) {
    when (langType) {
        LangType.FollowSystem -> {
            DataHelper.put(KEY_LANG_TYPE, NAME_FOLLOW_SYSTEM)
            DataHelper.remove(KEY_LANG) // 清理残留
        }

        is LangType.Specific -> {
            DataHelper.put(KEY_LANG_TYPE, langType.lang)
            DataHelper.put(KEY_LANG, langType.lang)
        }
    }
}

/**
 * 获取语言设置
 */
fun getLang(): LangType {
    val langType = DataHelper.getString(KEY_LANG_TYPE) ?: NAME_FOLLOW_SYSTEM
    return when (langType) {
        NAME_FOLLOW_SYSTEM -> LangType.FollowSystem
        else -> LangType.Specific(langType)
    }
}
//endregion