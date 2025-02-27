package com.munch1182.lib.floatwindow

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.Rect
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.BounceInterpolator
import androidx.core.view.isVisible
import kotlin.math.absoluteValue

object FWManager {

    // 直接持有view的对象
    private val map by lazy { mutableMapOf<String, FWWidget>() }

    private const val DEFAULT_ID = "window"

    fun canDrawOverlays(ctx: Context): Boolean {
        return Settings.canDrawOverlays(ctx)
    }

    /**
     * 内部持有了view的对象，所以view的context应该使用Context.applicationContext
     * 否则会造成内存泄漏
     */
    fun create(view: View, id: String = DEFAULT_ID): FWManager {
        val widget = map[id]
        if (widget == null) {
            map[id] = FWWidget(view).create()
        }
        return this
    }

    fun show(id: String = DEFAULT_ID) {
        map[id]?.show()
    }

    fun findView(id: String = DEFAULT_ID): View? {
        return map[id]?.view
    }

    fun isVisible(id: String = DEFAULT_ID): Boolean {
        return map[id]?.view?.isVisible ?: false
    }

    fun update(id: String = DEFAULT_ID, updateLP: WindowManager.LayoutParams.() -> Boolean) {
        val fwWidget = map[id] ?: return
        if (!updateLP(fwWidget.lp)) {
            return
        }
        fwWidget.view.context.wm()?.updateViewLayout(fwWidget.view, fwWidget.lp)
    }

    fun setEdgeMoveListener(
        id: String = DEFAULT_ID,
        listener: (leftTrueOrRight: Boolean) -> Unit
    ): FWManager {
        map[id]?.edgeMove(listener)
        return this
    }

    fun hide(id: String = DEFAULT_ID) {
        map[id]?.hide()
    }

    fun hideAll() {
        map.forEach { (_, v) -> v.hide() }
    }

    fun isDestroy(id: String = DEFAULT_ID): Boolean {
        return map[id]?.view == null
    }

    fun destroy(id: String = DEFAULT_ID) {
        map[id]?.destroy() ?: return
        map.remove(id)
    }

    fun destroyAll() {
        map.forEach { (id, _) -> destroy(id) }
    }
}

class FWWidget internal constructor(internal val view: View) {

    private val ctx = view.context.applicationContext
    internal val lp by lazy { WindowManager.LayoutParams() }
    private val canMoveRect by lazy { view.findCanMoveRect() }

    private val touchListener by lazy {
        FWTouchListener({ x, y, isDowned -> followMove(x, y, isDowned) }, { attachEdge(it) })
    }

    // 当view贴边后，仍然向该方向滑动的回调
    private var edgeMoveListener: ((leftTrueOrRight: Boolean) -> Unit)? = null

    internal fun edgeMove(listener: (leftTrueOrFalse: Boolean) -> Unit) {
        edgeMoveListener = listener
    }

    internal fun create(): FWWidget {
        if (view.isAttachedToWindow) {
            return this
        }
        updateLP()
        hide()
        view.setOnTouchListener(touchListener)
        ctx.wm()?.addView(view, lp)
        return this
    }

    internal fun show() {
        view.visibility = View.VISIBLE
    }

    internal fun hide() {
        view.visibility = View.GONE
    }

    internal fun destroy() {
        view.setOnTouchListener(null)
        ctx.wm()?.removeView(view)
    }

    private fun updateLP() {
        lp.apply {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            format = PixelFormat.TRANSPARENT
            gravity = Gravity.CENTER
            flags =
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        }
    }

    private fun followMove(x: Int, y: Int, isDowned: Boolean) {
        val vLoc = view.getInLocation()

        // 如果超出界限，显示没有问题单lp.x的值会一直增加
        // 导致下一次移动出现问题
        if ((vLoc.x == canMoveRect.left && x < 0) || (vLoc.x == canMoveRect.right && x > 0)) {
            // 长按滑动至贴边不触发回调
            if (isDowned) {
                edgeMoveListener?.invoke(x < 0)
            }
            return
        }
        if ((vLoc.y == canMoveRect.top && y < 0) || (vLoc.y == canMoveRect.bottom && y > 0)) {
            return
        }
        lp.x += x
        lp.y += y

        ctx.wm()?.updateViewLayout(view, lp)
    }

    private fun attachEdge(xDistance: Int) {
        if (xDistance == 0) return
        ValueAnimator.ofInt(0, xDistance).setDuration(if (xDistance > 0) 2000L else 1800L).apply {
            interpolator = BounceInterpolator()
            addUpdateListener { animation ->
                val vLoc = view.getInLocation()
                val x = animation.animatedValue as Int
                if ((vLoc.x == canMoveRect.left && x < 0) || (vLoc.x == canMoveRect.right && x > 0)) {
                    animation.cancel()
                    return@addUpdateListener
                }
                lp.x += x
                ctx.wm()?.updateViewLayout(view, lp)
            }
        }.start()
    }

}

internal class FWTouchListener(
    private val moveListener: ((x: Int, y: Int, isDowned: Boolean) -> Unit)? = null,
    private val attachEdgeListener: ((xDistance: Int) -> Unit)? = null,
) : View.OnTouchListener {
    private val lastMovePoint = Point()

    private var isMoving = false
    private var halfScreenWidth = -1
    private var endX = -1

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        v ?: return false
        event ?: return false


        if (halfScreenWidth == -1) {
            // 因为本身有宽度，所以左边判断范围少一点
            halfScreenWidth = (v.context.screenWidth() - v.width) / 2
        }
        if (endX == -1) {
            // endX = halfScreenWidth * 2 - v.width // 理论上是第一个，但是用第二个好用
            endX = halfScreenWidth * 2
        }

        val x = event.rawX.toInt()
        val y = event.rawY.toInt()

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isMoving = false
                lastMovePoint.set(x, y)
                return true
            }

            MotionEvent.ACTION_MOVE -> {

                val mX = x - lastMovePoint.x
                val mY = y - lastMovePoint.y
                lastMovePoint.set(x, y)

                moveListener?.invoke(mX, mY, !isMoving)
                isMoving = true
                return true
            }

            MotionEvent.ACTION_UP -> {
                if (!isMoving) {
                    v.performClick()
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

fun Context.wm(): WindowManager? {
    return kotlin.runCatching { getSystemService(Context.WINDOW_SERVICE) as? WindowManager }
        .getOrNull()
}

private fun View.getInLocation(): Point {
    val loc = IntArray(2)
    getLocationOnScreen(loc)
    return Point(loc[0], loc[1])
}

private fun Context.screenWidth(): Int {
    return resources.displayMetrics.widthPixels
}

private fun View.findCanMoveRect(): Rect {
    return Rect(
        0,
        context.getStatusBarHeight(),
        context.screenWidth() - width,
        // resources.displayMetrics.heightPixels只包含中间部分而不是整个屏幕
        resources.displayMetrics.heightPixels + context.getStatusBarHeight() - height
    )
}

@SuppressLint("InternalInsetResource")
private fun Context.getStatusBarHeight(): Int {
    return kotlin.runCatching {
        resources.getDimensionPixelSize(
            resources.getIdentifier(
                "status_bar_height",
                "dimen",
                "android"
            )
        )
    }.getOrNull() ?: 0
}

@SuppressLint("InternalInsetResource")
private fun Context.getNavigationBarHeight(): Int {
    return kotlin.runCatching {
        resources.getDimensionPixelSize(
            resources.getIdentifier("navigation_bar_height", "dimen", "android")
        )
    }.getOrNull() ?: 0
}