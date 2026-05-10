package com.munch1182.core.android

import android.content.Context
import android.os.Build

/**
 * 获取设备信息并格式化为字符串
 * @return 返回包含设备详细信息的字符串
 */
fun getDeviceInfo(context: Context = AppHelper): String {
    val displayMetrics = context.resources.displayMetrics
    val screenWidth = displayMetrics.widthPixels
    val screenHeight = displayMetrics.heightPixels
    val density = displayMetrics.density

    // 使用buildString构建设备信息字符串
    return buildString {
        // 添加制造商信息
        appendLine("Manufacturer: ${Build.MANUFACTURER}")
        // 添加设备型号信息
        appendLine("Model: ${Build.MODEL}")
        // 添加品牌信息
        appendLine("Brand: ${Build.BRAND}")
        // 添加设备名称信息
        appendLine("Device: ${Build.DEVICE}")
        // 添加产品名称信息
        appendLine("Product: ${Build.PRODUCT}")
        // 添加Android版本号和API级别
        appendLine("Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        // 添加屏幕分辨率信息
        appendLine("Resolution: $screenWidth x $screenHeight")
        // 添加屏幕密度信息
        appendLine("Density: $density dpi")
        // 添加CPU架构信息
        appendLine("CPU ABI: ${Build.SUPPORTED_ABIS.joinToString()}")
    }
}