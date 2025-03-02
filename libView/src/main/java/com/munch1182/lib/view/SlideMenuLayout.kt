package com.munch1182.lib.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes

class SlideMenuLayout @JvmOverloads constructor(
    context: Context,
    attr: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : FrameLayout(context, attr, defStyleAttr, defStyleRes) {

    private val TAG = "SlideMenuLayout"

    override fun onLayout(
        changed: Boolean, left: Int, top: Int, right: Int, bottom: Int
    ) {
        if (childCount == 1) {
            return super.onLayout(changed, left, top, right, bottom)
        }
        (0 until childCount - 1).forEach {
            val v = getChildAt(it)
            v.layout(0, 0, v.measuredWidth, v.measuredHeight)
        }
        // 将最后一个view作为菜单并且放在最后，未滑动前不可见
        val menu = menuView ?: return
        val menuTop = (height - menu.measuredHeight) / 2
        menu.layout(width - menu.measuredWidth, menuTop, width, menuTop + menu.measuredHeight)
    }

    val menuView: View?
        get() = getChildAt(childCount - 1)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(measuredWidth + (menuView?.measuredWidth ?: 0), measuredHeight)
    }


    // 如果子控件大于一个，则最后一个子控件作为菜单项
//    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
//        val count = childCount
//        if (count == 0) {
//            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
//            return
//        }
//        val wMode = MeasureSpec.getMode(widthMeasureSpec)
//        val hMode = MeasureSpec.getMode(heightMeasureSpec)
//
//        children.forEach { measureChild(it, widthMeasureSpec, heightMeasureSpec) }
//
//        var measureWidth = 0
//        var measureHeight = 0
//        when (wMode) {
//            MeasureSpec.EXACTLY -> measureWidth = MeasureSpec.getSize(widthMeasureSpec)
//            MeasureSpec.AT_MOST, MeasureSpec.UNSPECIFIED -> {
//                (0 until count - 1).forEach {
//                    val v = getChildAt(it)
//                    measureWidth = max(measureWidth, v.measuredWidth)
//                }
//            }
//        }
//        when (hMode) {
//            MeasureSpec.EXACTLY -> measureHeight = MeasureSpec.getSize(heightMeasureSpec)
//            MeasureSpec.AT_MOST, MeasureSpec.UNSPECIFIED -> {
//                (0 until count - 1).forEach {
//                    measureHeight = max(measureHeight, getChildAt(it).measuredHeight)
//                }
//            }
//        }
//
//        setMeasuredDimension(measureWidth, measureHeight)
//    }
}