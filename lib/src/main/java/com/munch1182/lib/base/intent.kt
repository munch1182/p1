package com.munch1182.lib.base

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings

fun Intent.wPName() = setData(Uri.fromParts("package", ctx.packageName, null))

fun shareTextIntent(text: String, title: String? = null): Intent = Intent.createChooser(
    Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }, title
)

fun appSetting() = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).wPName()

// https://developer.android.com/training/monitoring-device-state/doze-standby?hl=zh-cn#support_for_other_use_cases

//<editor-fold desc="battery">
/**
 * 是否在电池白名单中
 */
fun isBatteryWhiteList(): Boolean {
    val pw = ctx.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
    return pw.isIgnoringBatteryOptimizations(ctx.packageName)
}

/**
 * 请求加入电池白名单的intent
 *
 * 添加packageName(小米手机)反而会崩溃
 */
fun requestBatteryWhiteListIntent() = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
//</editor-fold>