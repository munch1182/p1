package com.munch1182.lib.base

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.abs

val Paint.height: Float
    get() = getFontMetrics().let { it.descent - it.ascent + it.leading }

/**
 * 以x,y作为中心点绘制文字
 */
fun Canvas.drawTextInCenter(text: String, x: Float, y: Float, paint: Paint) {
    val w = paint.measureText(text) / 2f
    val baseLineY = abs(paint.ascent() + paint.descent()) / 2f
    drawText(text, x - w, y + baseLineY, paint)
}

/**
 * 以x,y作为左侧中点绘制文字
 */
fun Canvas.drawTextInStartXCenterY(text: String, x: Float, y: Float, paint: Paint) {
    val baseLineY = abs(paint.ascent() + paint.descent()) / 2f
    drawText(text, x, y + baseLineY, paint)
}

/**
 * 包括超出baseline的文字范围
 * @return 返回文字范围，从(0,0)开始
 */
fun Paint.getTextRect(string: String): RectF {
    val fm = Paint.FontMetrics()
    getFontMetrics(fm)
    val rect = RectF()
    val width = measureText(string)
    val height = abs(fm.descent - fm.ascent)
    rect.set(0f, 0f, width, height)
    return rect
}