package com.munch1182.core.android

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build

val packInfo: PackageInfo get() = AppHelper.packageManager.getPackageInfo(AppHelper.packageName, PackageManager.GET_CONFIGURATIONS)
val versionName: String? get() = packInfo.versionName
val versionCodeCompat: Long
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packInfo.longVersionCode
    } else {
        @Suppress("DEPRECATION")
        packInfo.versionCode.toLong()
    }