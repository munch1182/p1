package com.munch1182.core.android

import android.content.Intent
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import androidx.core.net.toUri


fun Intent.withPkgUri() = apply { data = "package:${AppHelper.packageName}".toUri() }

/**
 * 应用设置intent
 */
val appSetting
    get() = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).withPkgUri()

/**
 * 判断开发者选项是否已打开
 */
val isDeveloperOpen get() = Settings.Global.getInt(AppHelper.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) != 0

/**
 * 跳转开发者选项的intent
 */
val developerOptionsIntent get() = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)

/**
 * 是否已打开定位开关
 */
fun isLocationEnable(manger: LocationManager = AppHelper.getSystemService(LocationManager::class.java)): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        manger.isLocationEnabled
    } else {
        runCatching {
            @Suppress("DEPRECATION")
            Settings.Secure.getInt(AppHelper.contentResolver, Settings.Secure.LOCATION_MODE) != Settings.Secure.LOCATION_MODE_OFF
        }.getOrNull() ?: false

    }
}

/**
 * 去打开/关闭定位开关
 */
val locationEnableIntent get() = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
