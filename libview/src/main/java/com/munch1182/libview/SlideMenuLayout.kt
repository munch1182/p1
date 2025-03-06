package com.munch1182.libview

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout

class SlideMenuLayout @JvmOverloads constructor(
    context: Context,
    attrsSet: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStylesRes: Int = 0
) : FrameLayout(context, attrsSet, defStyleAttr, defStylesRes) {

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        if (childCount < 2) {
            return super.onLayout(changed, left, top, right, bottom)
        }
        (0 until childCount - 1).forEach {
            getChildAt(it).apply { layout(left, top, left + width, top + height) }
        }
        getChildAt(childCount - 1).apply { layout(right, top, right + width, bottom + height) }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return super.onInterceptTouchEvent(ev)
    }
}