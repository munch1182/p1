package com.munch1182.lib.base

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.view.View
import android.view.ViewGroup

fun View.setDoubleClickListener(tag: Int, time: Long = 300L, listener: View.OnClickListener) {
    setOnClickListener {
        val lastClickTime = it.getTag(tag) as? Long?
        if (lastClickTime != null && System.currentTimeMillis() - lastClickTime <= time) {
            listener.onClick(it)
            return@setOnClickListener
        }
        it.setTag(tag, System.currentTimeMillis())
    }
}

fun newLayoutParams(w: Int = ViewGroup.LayoutParams.WRAP_CONTENT, h: Int = ViewGroup.LayoutParams.WRAP_CONTENT): ViewGroup.LayoutParams {
    return ViewGroup.LayoutParams(w, h)
}

fun newWWLP() = newLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
fun newMWLP() = newLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
fun newMMLP() = newLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
fun newWMLP() = newLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)

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
    color: Int,
    tl: Float = 0f,
    tr: Float = 0f,
    bl: Float = 0f,
    br: Float = 0f,
    strokeWidth: Int = 0,
    strokeColor: Int = Color.WHITE
): GradientDrawable {
    val gradientDrawable = GradientDrawable()
    val f = floatArrayOf(tl, tl, tr, tr, bl, bl, br, br)
    gradientDrawable.cornerRadii = f
    gradientDrawable.setColor(color)
    gradientDrawable.setStroke(strokeWidth, strokeColor)
    return gradientDrawable
}

fun newSelectDrawable(
    unselectedDrawable: Drawable?,
    selectedDrawable: Drawable?
): StateListDrawable {
    val drawable = StateListDrawable()
    drawable.addState(intArrayOf(-android.R.attr.state_selected), unselectedDrawable)
    drawable.addState(intArrayOf(android.R.attr.state_selected), selectedDrawable)
    return drawable
}