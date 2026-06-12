package com.munch1182.lib.common

/**
 * 将Long类型数值转换为RGB颜色字符串格式
 * @return 返回一个以"#"开头后跟6位十六进制数的字符串，表示RGB颜色值
 */
fun Long.toRGBColorStr() = String.format("#%06X", this and 0xFFFFFFFF)

/**
 * 将Long类型值转换为ARGB格式的颜色字符串
 *
 * @return 返回一个以"#"开头的8位十六进制颜色字符串，格式为#AARRGGBB
 */
fun Long.toARGBColorStr() = String.format("#%08X", this and 0xFFFFFFFF)
