package com.munch1182.core.android

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build

/**
 * 简化获取[PackageManager.GET_CONFIGURATIONS]相关的[PackageInfo]
 */
val packInfo: PackageInfo get() = AppHelper.packageManager.getPackageInfo(AppHelper.packageName, PackageManager.GET_CONFIGURATIONS)

/**
 * 获取版本号
 */
val versionName: String? get() = packInfo.versionName

/**
 * 获取版本号, 类型为[Long]
 */
val versionCodeCompat: Long
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packInfo.longVersionCode
    } else {
        @Suppress("DEPRECATION")
        packInfo.versionCode.toLong()
    }