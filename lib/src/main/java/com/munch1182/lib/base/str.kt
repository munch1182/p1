package com.munch1182.lib.base

import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import androidx.core.graphics.toColorInt
import kotlin.random.Random

fun String.withColor(color: String, start: Int = 0, length: Int = this.length): SpannableString {
    return SpannableString(this).apply { setSpan(ForegroundColorSpan(color.toColorInt()), start, length, Spannable.SPAN_INCLUSIVE_INCLUSIVE) }
}

fun newRandom(count: Int = 1): String {
    val sb = StringBuilder()
    repeat(count) { sb.append(Random.nextInt(0x4e00, 0x9fa5).toChar()) }
    return sb.toString()
}