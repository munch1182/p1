package com.munch1182.lib.base

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText

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
 * @param tr BottomRightRadius
 * @param bl BottomLeftRadius
 * @param br BottomRightRadius
 * @param strokeWidth 边框宽度
 * @param strokeColor 边框颜色
 */
fun newCornerDrawable(
    color: Int, tl: Float = 0f, tr: Float = 0f, bl: Float = 0f, br: Float = 0f, strokeWidth: Int = 0, strokeColor: Int = Color.WHITE
): GradientDrawable {
    val gradientDrawable = GradientDrawable()
    val f = floatArrayOf(tl, tl, tr, tr, bl, bl, br, br)
    gradientDrawable.cornerRadii = f
    gradientDrawable.setColor(color)
    gradientDrawable.setStroke(strokeWidth, strokeColor)
    return gradientDrawable
}

fun newSelectDrawable(
    unselectedDrawable: Drawable?, selectedDrawable: Drawable?
): StateListDrawable {
    val drawable = StateListDrawable()
    drawable.addState(intArrayOf(-android.R.attr.state_selected), unselectedDrawable)
    drawable.addState(intArrayOf(android.R.attr.state_selected), selectedDrawable)
    return drawable
}

fun EditText.showSoftInput(delay: Long = 300L) {
    isFocusable = true
    isFocusableInTouchMode = true
    postDelayed({
        requestFocus()
        requestFocusFromTouch()
        val im = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        im?.showSoftInput(this, 0)
    }, delay)
}

fun EditText.hideSoftInput() {
    val im = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    im?.hideSoftInputFromWindow(windowToken, 0)
}