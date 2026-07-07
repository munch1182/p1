package com.munch1182.lib.android

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import androidx.core.net.toUri

/**
 * 跳转至指定uri
 */
fun navigate2DeepLink(uri: String, ctx: Context = AppHelper) {
    ctx.startActivity(Intent(Intent.ACTION_VIEW, uri.toUri()).apply {
        if (ctx !is Activity) newTask
    })
}

/**
 * [Intent]附加带包名的uri, 以在有些intent中调整至本app相关的页面;
 *
 * 注意: 不能乱用, 有些手机有些intent不应该附带也附带可能会引起崩溃
 */
fun Intent.withPkgUri() = apply { data = "package:${AppHelper.packageName}".toUri() }

/**
 * [Intent]启动在另一个task中
 */
val Intent.newTask get() = apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }

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
val enableLocationIntent get() = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)

/**
 * 获取[Intent]中的`Parcelable`对象, 兼容低版本
 */
inline fun <reified T> Intent.getParcelableCompat(name: String, clazz: Class<T>): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra<T>(name, clazz)
    } else {
        @Suppress("DEPRECATION") getParcelableExtra(name)
    }
}

inline fun <reified T> Intent.getParcelableArrayCompat(name: String, clazz: Class<T>): Array<out T>? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableArrayExtra<T>(name, clazz)
    } else {
        @Suppress("DEPRECATION", "UNCHECKED_CAST")
        getParcelableArrayExtra(name) as Array<out T>?
    }
}
