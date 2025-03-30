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

        // 最大水平滑动距离
        override fun clampViewPositionHorizontal(child: View, left: Int, dx: Int): Int {
            return min(0, max(-menuView.width, left))
        }

        // 抬起手指之后滑动 xvel/yvel表示加速度
        override fun onViewReleased(releasedChild: View, xvel: Float, yvel: Float) {
            val offset = (releasedChild.width - releasedChild.right).toFloat() / menuView.width.toFloat()
            val judgeOpen = offset >= 0.5f
            val finalLeft = if (judgeOpen) -menuView.width else 0
            dragHelper.settleCapturedViewAt(finalLeft, releasedChild.top)
            invalidate()
        }

        // 水平运动范围，返回0表示不允许滑动
        override fun getViewHorizontalDragRange(child: View): Int {
            return if (child == contentView) menuView.width else 0
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

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        contentView.layout(0, 0, measuredWidth, contentView.measuredHeight)
        menuView.layout(measuredWidth, 0, measuredWidth + menuView.measuredWidth, contentView.measuredHeight)
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