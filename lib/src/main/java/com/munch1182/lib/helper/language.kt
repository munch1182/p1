package com.munch1182.lib.helper

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.os.LocaleListCompat
import java.util.Locale

fun String.toLocale(): Locale {
    return if (this.contains("-")) {
        this.split("-").let { Locale(it[0], it[1], it.getOrNull(2) ?: "") }
    } else {
        Locale(this)
    }
}

interface LanguageHelperImpl {

    val curr: Locale

    // 获取当前系统语言
    fun currSystemLocale(): Locale = Locale.getDefault()

    // 获取当前应用偏好语言，当调用[updateLocale]后，此返回会同步更改
    fun currAppLocale(act: Activity = currAct) = act.resources.configuration.locales.get(0)

    fun updateLocale(lang: String) {
        val tag = LocaleListCompat.forLanguageTags(lang)
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
    fun attachBaseContext(newBase: Context, theme: Resources.Theme? = null): LocaleCtxWrapper {
        val conf = newBase.resources.configuration
        conf.setLocales(LocaleList(curr))
        val newCtx = newBase.createConfigurationContext(conf)
        return LocaleCtxWrapper(newCtx, conf, theme ?: newCtx.theme)
    }
}

class LocaleCtxWrapper(ctx: Context, private val conf: Configuration, theme: Resources.Theme?) : ContextThemeWrapper(ctx, theme) {
    override fun applyOverrideConfiguration(overrideConfiguration: Configuration?) {
        overrideConfiguration?.setTo(conf)
        super.applyOverrideConfiguration(overrideConfiguration)
    }
}
