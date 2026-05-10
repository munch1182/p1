package com.munch1182.core.android

import android.content.Context
import android.content.res.Resources
import java.util.Locale

/**
 * 返回设备系统的首选 locale（不受应用内切换影响）
 */
fun getDeviceLocale(): Locale = Resources.getSystem().configuration.locales.get(0) ?: Locale.getDefault()

/**
 * 返回应用内的 locale
 */
fun getAppLocale(context: Context = AppHelper): Locale = context.resources.configuration.locales.get(0) ?: Locale.getDefault()