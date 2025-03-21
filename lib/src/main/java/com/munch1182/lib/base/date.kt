package com.munch1182.lib.base

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Date.toString(format: String = "yyyy-MM-dd HH:mm:ss"): String {
    return SimpleDateFormat(format, Locale.getDefault()).format(this)
}

fun nowStr(format: String = "yyyy-MM-dd HH:mm:ss") = Date().toString(format)