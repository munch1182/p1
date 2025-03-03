package com.munch1182.lib

import java.text.SimpleDateFormat
import java.util.Locale

fun simpleDateStr(pattern: String = "yyyyMMddHHmmss"): String? {
    return SimpleDateFormat(pattern, Locale.getDefault()).format(System.currentTimeMillis())
}