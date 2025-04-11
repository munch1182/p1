package com.munch1182.p1.base

import android.content.Context
import com.munch1182.lib.AppHelper
import com.munch1182.lib.base.log
import com.munch1182.lib.helper.LanguageHelperImpl
import com.munch1182.lib.helper.toLocale
import java.util.Locale

object LanguageHelper : LanguageHelperImpl {

    private var _curr: Locale
    private val log = log(false)

    override val curr: Locale get() = _curr

    private const val KEY_LANG_STR = "LanguageHelper_LANG_STR"

    init {
        _curr = load()?.toLocale() ?: currSystemLocale()
        log.logStr("curr locale: $_curr")
    }

    override fun updateLocale(lang: String) {
        log.logStr("updateLocale: $_curr -> $lang")
        save(lang)
        _curr = lang.toLocale()
        super.updateLocale(lang)
    }

    private fun save(lang: String) {
        AppHelper.getSharedPreferences("LANG", Context.MODE_PRIVATE).edit().putString(KEY_LANG_STR, lang).apply()
    }

    private fun load(): String? {
        return AppHelper.getSharedPreferences("LANG", Context.MODE_PRIVATE).getString(KEY_LANG_STR, null)
    }
}