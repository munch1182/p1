package com.munch1182.lib.base

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Date.toStr(format: String = "yyyy-MM-dd HH:mm:ss"): String {
    return SimpleDateFormat(format, Locale.getDefault()).format(this)
}

fun Long.toDateStr(format: String = "yyyy-MM-dd HH:mm:ss") = Date(this).toStr(format)

fun nowStr(format: String = "yyyy-MM-dd HH:mm:ss") = Date().toStr(format)