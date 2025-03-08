package com.munch1182.lib.helper

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings

// https://developer.android.com/training/monitoring-device-state/doze-standby?hl=zh-cn#support_for_other_use_cases

/**
 * 是否在电池白名单中
 */
fun Context.isBatteryOptimization(): Boolean {
    val pw = getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
    return pw.isIgnoringBatteryOptimizations(packageName)
}

/**
 * 请求加入电池白名单的intent
 *
 * 添加packageName(小米手机)反而会崩溃
 */
fun Context.requestBatteryOptimizationIntent() = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)