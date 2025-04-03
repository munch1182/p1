package com.munch1182.lib.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.customview.widget.ViewDragHelper
import com.munch1182.lib.widget.SwapMenuLayout.OnStateChangeListener
import kotlin.math.abs
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
            onStateChange?.onStateChange(this@SwapMenuLayout, state)
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

        override fun getViewHorizontalDragRange(child: View): Int {
            return dragHelper.touchSlop
        }
    }
    private var down = PointF()

    init {
        dragHelper = ViewDragHelper.create(this, 1f, dragCallback)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> down.set(ev.x, ev.y)
            MotionEvent.ACTION_MOVE -> {
                val xSlop = abs(ev.x - down.x)
                val ySlop = abs(ev.y - down.y)
                if (xSlop > dragHelper.touchSlop && xSlop > ySlop) {
                    parent.requestDisallowInterceptTouchEvent(true)
                }
            }
        }
        return super.dispatchTouchEvent(ev)
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

    // 应该在OnStateChangeListener中获取
    val isOpen: Boolean get() = if (isR2L) (menuView.translationX == menuView.width.toFloat()) else (menuView.translationX < 0f)

    fun open() {
        dragHelper.smoothSlideViewTo(contentView, if (isR2L) menuView.width else -menuView.width, contentView.top)
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
        menuView.translationX = 0f
        dragHelper.abort()
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
            invalidate()
        }
    }

    @FunctionalInterface
    fun interface OnStateChangeListener {
        fun onStateChange(view: SwapMenuLayout, state: Int)
    }

    class LimitHelper {
        private var _currOpen: SwapMenuLayout? = null
        private var _currPos: Int? = null
        private val stateChange = OnStateChangeListener { view, state ->
            if (state == ViewDragHelper.STATE_IDLE) {
                if (view.isOpen) {
                    _currOpen = view
                    _currPos = view.tag as? Int
                }
            } else if (_currOpen != view) {
                _currOpen?.close()
            }
        }

        fun bind(swap: SwapMenuLayout, pos: Int) {
            swap.tag = pos
            swap.setOnStateChangeListener(stateChange)
        }

        val currPos: Int? get() = _currPos
        val currOpen: SwapMenuLayout? get() = _currOpen
    }
}