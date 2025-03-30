package com.munch1182.lib.helper

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.munch1182.lib.base.ctx
import com.munch1182.lib.base.log
import java.util.Locale


interface LanguageHelperImpl {

    fun currLocale(): Locale = AppCompatDelegate.getApplicationLocales().get(0) ?: ctx.resources.configuration.locales.get(0) ?: Locale.getDefault()

    fun updateLocale(lang: String) {
        val tag = LocaleListCompat.forLanguageTags(lang)
        log().logStr("updateLocale: $lang -> $tag")
        AppCompatDelegate.setApplicationLocales(tag)
    }
}
