package com.munch1182.lib.base

import android.graphics.Rect
import android.graphics.RectF
import android.view.View

val Rect.middleHeight: Int
    get() = (bottom - top) / 2 + top
val Rect.middleWidth: Int
    get() = (right - left) / 2 + left
val RectF.middleHeight: Float
    get() = (bottom - top) / 2f + top
val RectF.middleWidth: Float
    get() = (right - left) / 2f + left

fun View.toRect() = Rect(left, top, right, bottom)

fun Rect.toRectF() = RectF(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())