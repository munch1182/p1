package com.munch1182.android.lib.base

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import com.munch1182.android.lib.AppHelper

fun <T> Intent.getParcelableCompat(name: String, clazz: Class<T>): T? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    getParcelableExtra(name, clazz)
} else {
    @Suppress("DEPRECATION") getParcelableExtra(name)
}

fun Intent.wPName() = setData(Uri.fromParts("package", ctx.packageName, null))

fun shareTextIntent(text: String, title: String? = null): Intent = Intent.createChooser(
    Intent(Intent.ACTION_SEND).apply {
        setType("text/plain")
        putExtra(Intent.EXTRA_TEXT, text)
    }, title
)

fun shareImage(uri: Uri, title: String? = null): Intent = Intent.createChooser(
    Intent(Intent.ACTION_SEND).apply {
        setType("image/*")
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }, title
)

fun copyText(text: String) {
    clipManager.setPrimaryClip(ClipData.newPlainText("text", text))
}

fun appSetting() = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).wPName()
fun locSetting() = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
fun blueSetting() = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)

fun selectFile(type: String) = Intent(Intent.ACTION_GET_CONTENT).setType(type)
fun selectDir() = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)

@RequiresApi(Build.VERSION_CODES.R)
fun managerAllFiles() = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
    setData("package:${AppHelper.packageName}".toUri())
}

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
