package com.munch1182.lib.base

import android.graphics.Canvas
import android.graphics.Paint
import kotlin.math.abs

val Paint.height: Float
    get() = getFontMetrics().let { it.descent - it.ascent + it.leading }

fun Canvas.drawTextInCenter(text: String, x: Float, y: Float, paint: Paint) {
    val w = paint.measureText(text) / 2f
    val baseLineY = abs(paint.ascent() + paint.descent()) / 2f
    drawText(text, x - w, y + baseLineY, paint)
}

fun Canvas.drawTextInStartXCenterY(text: String, x: Float, y: Float, paint: Paint) {
    val baseLineY = abs(paint.ascent() + paint.descent()) / 2f
    drawText(text, x, y + baseLineY, paint)
}