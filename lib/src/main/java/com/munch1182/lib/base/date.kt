package com.munch1182.lib.base

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

fun Date.toStr(format: String = "yyyy-MM-dd HH:mm:ss"): String {
    return SimpleDateFormat(format, Locale.getDefault()).format(this)
}

fun Long.toDateStr(format: String = "yyyy-MM-dd HH:mm:ss") = Date(this).toStr(format)

fun nowStr(format: String = "yyyy-MM-dd HH:mm:ss") = Date().toStr(format)

fun Long.toDurationStr(pattern: String = "HH:mm:ss"): String =
    SimpleDateFormat(pattern, Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(this)

fun Long.toDurationStrSmart(): String {
    val pattern = if (this / 1000 / 60 / 60 > 0) "HH:mm:ss" else "mm:ss"
    return SimpleDateFormat(pattern, Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(this)
}