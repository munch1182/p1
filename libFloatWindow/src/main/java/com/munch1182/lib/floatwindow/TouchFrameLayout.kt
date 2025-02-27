package com.munch1182.lib.floatwindow

import android.content.Context
import android.graphics.Color
import android.graphics.Point
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import kotlin.math.absoluteValue

class TouchFrameLayout @JvmOverloads constructor(
    context: Context,
    attr: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : FrameLayout(context, attr, defStyleAttr, defStyleRes) {
    private val lastMovePoint = Point()

    private var isMoveJudge = false
    private var isMoving = false
    private val halfScreenWidth by lazy { (context.screenWidth() - width) / 2 }
    private val endX by lazy { halfScreenWidth * 2 }
    private var moveListener: ((x: Int, y: Int, isMove: Boolean) -> Unit)? = null
    private var attachEdgeListener: ((xDistance: Int) -> Unit)? = null

    init {
        setBackgroundColor(Color.TRANSPARENT)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        val intercept = when (ev?.action) {
            MotionEvent.ACTION_DOWN -> {
                isMoveJudge = false
                false
            }

            MotionEvent.ACTION_MOVE -> {
                isMoveJudge = true
                true
            }

            MotionEvent.ACTION_UP -> {
                isMoveJudge
            }

            else -> false
        }
        return intercept || super.onInterceptTouchEvent(ev)
    }

    fun setMoveListener(listener: (x: Int, y: Int, isMove: Boolean) -> Unit): TouchFrameLayout {
        this.moveListener = listener
        return this
    }

    fun setAttachEdgeListener(listener: (xDistance: Int) -> Unit): TouchFrameLayout {
        this.attachEdgeListener = listener
        return this
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val v = this
        event ?: return false

        val x = event.rawX.toInt()
        val y = event.rawY.toInt()

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isMoving = false
                lastMovePoint.set(x, y)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                // 因为没有拦截DOWN事件，所以需要在这里判断并赋值
                if (!isMoving) {
                    isMoving = true
                    lastMovePoint.set(x, y)
                    return true
                }
                val mX = x - lastMovePoint.x
                val mY = y - lastMovePoint.y
                lastMovePoint.set(x, y)
                moveListener?.invoke(mX, mY, !isMoving)
                isMoving = true
                return true
            }

            MotionEvent.ACTION_UP -> {
                if (!isMoving) {
                    performClick()
                    return false
                }
                isMoving = false
                val loc = v.getInLocation()
                val xDistance = if (loc.x > halfScreenWidth) {
                    (endX - loc.x).absoluteValue
                } else {
                    loc.x * -1
                }
                attachEdgeListener?.invoke(xDistance)
            }
        }
        return false
    }
}
