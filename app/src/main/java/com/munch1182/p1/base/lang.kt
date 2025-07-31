package com.munch1182.p1.base

import com.munch1182.lib.AppHelper
import com.munch1182.lib.base.launchIO
import com.munch1182.lib.base.log
import com.munch1182.lib.helper.LanguageHelperImpl
import com.munch1182.lib.helper.SPHelper
import com.munch1182.lib.helper.toLocale
import kotlinx.coroutines.runBlocking
import java.util.Locale

object LanguageHelper : LanguageHelperImpl {

    private val languageDataHelper = SPHelper("LanguageDataHelper")
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
        AppHelper.launchIO { languageDataHelper.put(KEY_LANG_STR, lang) }
    }

    private fun load(): String? {
        return runBlocking { languageDataHelper.get(KEY_LANG_STR, "").takeIf { it?.isNotEmpty() == true } }
    }
}