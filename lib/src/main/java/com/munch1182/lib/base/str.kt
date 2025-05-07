package com.munch1182.lib.base

import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import androidx.core.graphics.toColorInt

fun String.withColor(color: String, start: Int = 0, length: Int = this.length): SpannableString {
    return SpannableString(this).apply { setSpan(ForegroundColorSpan(color.toColorInt()), start, length, Spannable.SPAN_INCLUSIVE_INCLUSIVE) }
}