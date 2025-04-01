package com.munch1182.lib.helper

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.os.LocaleListCompat
import com.munch1182.lib.base.log
import java.util.Locale
import com.munch1182.lib.helper.curr as currAct

fun String.toLocale(): Locale {
    return if (this.contains("-")) {
        this.split("-").let { Locale(it[0], it[1], it.getOrNull(2) ?: "") }
    } else {
        Locale(this)
    }
}

interface LanguageHelperImpl {

    val curr: Locale

    // 获取当前语言，应该使用Activity的上下文，而不是app
    fun currLocale(act: Activity = currAct): Locale = act.resources.configuration.locales.get(0) ?: Locale.getDefault()

    fun updateLocale(lang: String) {
        val tag = LocaleListCompat.forLanguageTags(lang)
        log().logStr("updateLocale: $lang -> $tag")
        AppCompatDelegate.setApplicationLocales(tag)
    }

    /**
     * 是否需要调用这个方法与Activity的父类及theme相关，考虑到兼容性应该实现
     *
     * object LanguageHelper : LanguageHelperImpl
     *
     * abstract class BaseActivity : FragmentActivity() {
     *
     *     override fun attachBaseContext(newBase: Context) {
     *         super.attachBaseContext(LanguageHelper.attachBaseContext(newBase))
     *     }
     * }
     */
    fun attachBaseContext(newBase: Context): LocaleCtxWrapper {
        val conf = newBase.resources.configuration
        conf.setLocales(LocaleList(curr))
        val newCtx = newBase.createConfigurationContext(conf)
        return LocaleCtxWrapper(newCtx, conf)
    }
}

class LocaleCtxWrapper(ctx: Context, private val conf: Configuration) : ContextThemeWrapper(ctx, ctx.theme) {
    override fun applyOverrideConfiguration(overrideConfiguration: Configuration?) {
        overrideConfiguration?.setTo(conf)
        super.applyOverrideConfiguration(overrideConfiguration)
    }
}
