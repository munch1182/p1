package com.munch1182.core.android

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.os.StrictMode.VmPolicy

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

/**
 * 启用严格模式， 检测违规信息在logcat中输出， tag: StrictMode;
 * 只能在debug模式下使用;
 *
 * 对于某些必要操作但被视为违规（比如： 在第三方库要求必须在主线程加载so库):
 * 可以将开启放在加载动作之后， 或者使用@SuppressLint("DiscouragedPrivateApi")关闭
 */
fun enableStrictMode() {
    StrictMode.setThreadPolicy(
        ThreadPolicy.Builder()
            .detectDiskReads() // 检测主线程中的磁盘读操作
            .detectDiskWrites() // 检测主线程中的磁盘写操作
            .detectNetwork() // 检测主线程中的网络访问
            .detectAll() // 也可以使用此方法来检测所有已知问题
            .penaltyLog() // 发生违规时，打印 Log 日志
            .build()
    )
    StrictMode.setVmPolicy(
        VmPolicy.Builder()
            .detectActivityLeaks() // 检测 Activity 内存泄漏
            .detectLeakedSqlLiteObjects() // 检测 SQLite 对象泄漏
            .detectLeakedClosableObjects() // 检测未关闭的 Closable 对象泄漏
            .penaltyLog() // 发生违规时，打印 Log 日志
            .build()
    )
}