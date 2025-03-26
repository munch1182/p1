package com.munch1182.lib.helper

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.munch1182.lib.base.ctx
import java.util.Locale


interface LanguageHelperImpl {

    fun currLocale(): Locale = AppCompatDelegate.getApplicationLocales().get(0) ?: ctx.resources.configuration.locales.get(0) ?: Locale.getDefault()

    fun updateLocale(lang: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang))
    }

    fun attachContent(baseCtx: Context) {

    }
}
