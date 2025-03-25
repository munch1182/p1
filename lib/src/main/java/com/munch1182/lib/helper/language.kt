package com.munch1182.lib.helper

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

open class LanguageHelper {

    fun currLocale(): Locale = if (AppCompatDelegate.getApplicationLocales().isEmpty.not()) {
        AppCompatDelegate.getApplicationLocales()[0]
    } else {
        null
    } ?: Locale.getDefault()

    @Deprecated("mut set first")
    fun allLocales(): Array<Locale> {
        val compat = AppCompatDelegate.getApplicationLocales()
        if (compat.isEmpty) return arrayOf()
        return Array(compat.size()) { compat.get(it)!! }
    }

    fun updateLocale(langs: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(langs))
    }
}