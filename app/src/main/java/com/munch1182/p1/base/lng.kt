package com.munch1182.p1.base

import android.content.Context
import com.munch1182.lib.AppHelper
import com.munch1182.lib.base.log
import com.munch1182.lib.helper.LanguageHelperImpl
import com.munch1182.lib.helper.toLocale
import java.util.Locale

object LanguageHelper : LanguageHelperImpl {

    private var _curr: Locale
    private val log = log()

    override val curr: Locale get() = _curr

    init {
        _curr = load()?.toLocale() ?: Locale.getDefault()
        log.logStr("curr locale: $_curr")
    }

    override fun updateLocale(lang: String) {
        _curr = lang.toLocale()
        save(lang)
        super.updateLocale(lang)
    }

    private fun save(lang: String) {
        AppHelper.getSharedPreferences("LANG", Context.MODE_PRIVATE).edit().putString("LANG_STR", lang).apply()
    }

    private fun load(): String? {
        return AppHelper.getSharedPreferences("LANG", Context.MODE_PRIVATE).getString("LANG_STR", null)
    }
}