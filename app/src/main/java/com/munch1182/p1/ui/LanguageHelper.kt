package com.munch1182.p1.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.munch1182.core.android.getAppLocale
import com.munch1182.p1.base.logFailure
import com.munch1182.p1.data.getLang
import com.munch1182.p1.data.saveLang
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

object LanguageHelper {
    private val _currLangType = MutableStateFlow<LangType>(LangType.FollowSystem)
    private val _currLocale = MutableStateFlow(getAppLocale())

    val currLangType = _currLangType.asStateFlow()
    val currLocale = _currLocale.asStateFlow()

    init {
        _currLocale.value = getAppLocale()
        _currLangType.value = getLang()
    }

    fun reset() = switch(LangType.FollowSystem)

    fun switch(lang: String, keepLocale: Boolean = false) = switch(LangType.Specific(lang), keepLocale)

    /**
     * 切换语言
     * @param langType 语言类型
     * @param keepLocale 是否只切换语言而不更改地区(影响传入Locale的方法，比如String.format(.., Locale.getDefault()))
     */
    fun switch(langType: LangType, keepLocale: Boolean = false) {
        _currLangType.value = langType

        val newLocales = when (langType) {
            is LangType.FollowSystem -> LocaleListCompat.getEmptyLocaleList() // 官方推荐：传空列表代表清空应用专属设置，回归系统默认
            is LangType.Specific -> {
                val curr = _currLocale.value
                val builder = Locale.Builder()
                if (keepLocale) {
                    runCatching { builder.setLocale(curr) }.logFailure("builder.setLocale(${curr.language})")
                }
                builder.setLanguage(langType.lang)
                LocaleListCompat.create(builder.build())
            }
        }

        if (newLocales.isEmpty) {
            _currLocale.value = Locale.getDefault()
        } else {
            _currLocale.value = newLocales.get(0) ?: Locale.getDefault()
        }

        saveLang(_currLangType.value)
        AppCompatDelegate.setApplicationLocales(newLocales)
    }
}

sealed class LangType {
    object FollowSystem : LangType()
    data class Specific(val lang: String) : LangType()
}