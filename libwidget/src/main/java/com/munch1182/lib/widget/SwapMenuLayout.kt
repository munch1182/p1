package com.munch1182.lib.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.customview.widget.ViewDragHelper
import kotlin.math.max
import kotlin.math.min

/**
 * 必须包含两个view，一个作为contentView且需要占满(用于处理触摸事件)，另一个作为menuView固定宽度
 */
class SwapMenuLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

    private lateinit var menuView: View
    private lateinit var contentView: View
    private lateinit var dragHelper: ViewDragHelper
    private var onStateChange: OnStateChangeListener? = null
    private var dragCallback = object : ViewDragHelper.Callback() {

        override fun onViewDragStateChanged(state: Int) {
            super.onViewDragStateChanged(state)
            onStateChange?.onStateChange(state)
        }

        // 是否可以处理该view的滑动
        override fun tryCaptureView(child: View, pointerId: Int): Boolean {
            return child == contentView
        }

        // 控制横向方向的拖动，返回对横向拖动的修正值，不修正直接返回left即可
        override fun clampViewPositionHorizontal(child: View, left: Int, dx: Int): Int {
            if (isR2L) return max(0, min(left, menuView.width))// 允许滑动从[0, menuView.width]的距离, left只能在其中，超过边界值会被修正
            return min(max(-menuView.width, left), 0) /// 允许滑动从[-menuView.width, 0]的距离, left只能在其中，超过边界值会被修正
        }

        // 抬起手指之后滑动，xvel/yvel表示加速度
        override fun onViewReleased(releasedChild: View, xvel: Float, yvel: Float) {
            val judgeWidth = if (isR2L) releasedChild.left else (releasedChild.width - releasedChild.right)
            val offset = judgeWidth.toFloat() / menuView.width.toFloat()
            val judgeOpen = offset >= 0.5f
            val finalLeft = if (isR2L) {
                if (judgeOpen) menuView.width else 0
            } else {
                if (judgeOpen) -menuView.width else 0
            }
            dragHelper.settleCapturedViewAt(finalLeft, releasedChild.top)

            invalidate()
        }

        override fun onViewPositionChanged(changedView: View, left: Int, top: Int, dx: Int, dy: Int) {
            menuView.translationX = left.toFloat()
        }
    }

    init {
        dragHelper = ViewDragHelper.create(this, 1f, dragCallback)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return dragHelper.shouldInterceptTouchEvent(ev)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        dragHelper.processTouchEvent(event)
        return true
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        if (childCount != 2) {
            throw RuntimeException("SwipeMenuLayout must have 2 child view only.")
        }
        contentView = getChildAt(0)
        menuView = getChildAt(1)
    }

    val isOpen: Boolean get() = (menuView.translationX < 0f)

    fun open() {
        dragHelper.smoothSlideViewTo(contentView, -menuView.width, contentView.top)
        invalidate()
    }

    fun close() {
        dragHelper.smoothSlideViewTo(contentView, 0, contentView.top)
        invalidate()
    }

    fun toggle() = if (isOpen) close() else open()

    fun setOnStateChangeListener(l: OnStateChangeListener? = null): SwapMenuLayout {
        this.onStateChange = l
        return this
    }

    private val isR2L: Boolean get() = resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        if (!isR2L) {
            contentView.layout(0, 0, measuredWidth, contentView.measuredHeight)
            menuView.layout(measuredWidth, 0, measuredWidth + menuView.measuredWidth, contentView.measuredHeight)
        } else {
            contentView.layout(0, 0, measuredWidth, contentView.measuredHeight)
            menuView.layout(-menuView.measuredWidth, 0, 0, contentView.measuredHeight)
        }
    }

    override fun computeScroll() {
        super.computeScroll()
        // 触发dragHelper.settleCapturedViewAt的移动
        if (dragHelper.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this)
        }
    }

    @FunctionalInterface
    fun interface OnStateChangeListener {
        fun onStateChange(state: Int)
    }
}