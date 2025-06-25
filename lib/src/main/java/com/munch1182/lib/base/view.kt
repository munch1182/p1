package com.munch1182.lib.base

import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup

fun View.toRect() = Rect(left, top, right, bottom)

val lpW: Int = ViewGroup.LayoutParams.WRAP_CONTENT
val lpM: Int = ViewGroup.LayoutParams.MATCH_PARENT

val lpWW = ViewGroup.LayoutParams(lpW, lpW)
val lpWM = ViewGroup.LayoutParams(lpW, lpM)
val lpMW = ViewGroup.LayoutParams(lpM, lpW)
val lpMM = ViewGroup.LayoutParams(lpM, lpM)

/**
 * 生成一个圆角的Drawable
 *
 * @param color Drawable颜色
 * @param tl TopLeftRadius
 * @param tr TopRightRadius
 * @param bl BottomLeftRadius
 * @param br BottomRightRadius
 * @param strokeWidth 边框宽度
 * @param strokeColor 边框颜色
 */
fun newCornerDrawable(
    tl: Float = 0f, tr: Float = 0f, bl: Float = 0f, br: Float = 0f, strokeWidth: Int = 1, strokeColor: Int = Color.BLACK
): GradientDrawable {
    val shape = GradientDrawable()
    // tlx, tly, trx, try, blx, bly, brx, bry
    val f = floatArrayOf(tl, tl, tr, tr, bl, bl, br, br)
    shape.cornerRadii = f
    shape.setStroke(strokeWidth, strokeColor)
    return shape
}

fun newCornerDrawable(corner: Float = 8f, strokeWidth: Int = 1, strokeColor: Int = Color.BLACK): GradientDrawable {
    return newCornerDrawable(corner, corner, corner, corner, strokeWidth, strokeColor)
}

fun newSelectDrawable(
    unselectedDrawable: Drawable?, selectedDrawable: Drawable?
): StateListDrawable {
    val drawable = StateListDrawable()
    drawable.addState(intArrayOf(-android.R.attr.state_selected), unselectedDrawable)
    drawable.addState(intArrayOf(android.R.attr.state_selected), selectedDrawable)
    return drawable
}

val Int.specMode get() = MeasureSpec.getMode(this)
val Int.specSize get() = MeasureSpec.getSize(this)