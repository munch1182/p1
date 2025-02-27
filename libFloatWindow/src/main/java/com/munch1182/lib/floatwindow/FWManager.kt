package com.munch1182.lib.floatwindow

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.Rect
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.BounceInterpolator
import androidx.core.view.isVisible

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

    fun update(id: String = DEFAULT_ID, update: WindowManager.LayoutParams.() -> Boolean) {
        updateView(id) { _, lp -> update(lp) }
    }

    fun updateView(id: String = DEFAULT_ID, update: (View, WindowManager.LayoutParams) -> Boolean) {
        val fwWidget = map[id] ?: return
        if (!update(fwWidget.view, fwWidget.lp)) {
            return
        }
        fwWidget.update()
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

class FWWidget internal constructor(val view: View) {

    private val ctx = view.context.applicationContext
    internal val lp by lazy { WindowManager.LayoutParams() }
    private var canMoveRect = view.findCanMoveRect()
    private val tfl by lazy { TouchFrameLayout(ctx).apply { addView(view) } }

    // 当view贴边后，仍然向该方向滑动的回调
    private var edgeMoveListener: ((leftTrueOrRight: Boolean) -> Unit)? = null

    internal fun edgeMove(listener: (leftTrueOrFalse: Boolean) -> Unit) {
        edgeMoveListener = listener
    }

    internal fun create(): FWWidget {
        if (tfl.isAttachedToWindow) {
            return this
        }
        updateLP()
        hide()
        tfl.setMoveListener { x, y, isMove -> followMove(x, y, isMove) }
            .setAttachEdgeListener { attachEdge(it) }
        ctx.wm()?.addView(tfl, lp)
        updateCanMoveRect()
        return this
    }

    internal fun update() {
        ctx.wm()?.updateViewLayout(tfl, lp)
        updateCanMoveRect()
    }

    internal fun show() {
        tfl.visibility = View.VISIBLE
    }

    internal fun hide() {
        tfl.visibility = View.GONE
    }

    internal fun destroy() {
        tfl.setOnTouchListener(null)
        ctx.wm()?.removeView(tfl)
    }

    private fun updateCanMoveRect() {
        canMoveRect = view.findCanMoveRect()
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

        update()
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
                update()
            }
        }.start()
    }

}

fun Context.wm(): WindowManager? {
    return kotlin.runCatching { getSystemService(Context.WINDOW_SERVICE) as? WindowManager }
        .getOrNull()
}

internal fun View.getInLocation(): Point {
    val loc = IntArray(2)
    getLocationOnScreen(loc)
    return Point(loc[0], loc[1])
}

internal fun View.findCanMoveRect(): Rect {
    return Rect(
        0,
        context.getStatusBarHeight(),
        context.screenWidth() - width,
        // resources.displayMetrics.heightPixels只包含中间部分而不是整个屏幕
        resources.displayMetrics.heightPixels + context.getStatusBarHeight() - height
    )
}


internal fun Context.screenWidth(): Int {
    return resources.displayMetrics.widthPixels
}


@SuppressLint("InternalInsetResource")
internal fun Context.getStatusBarHeight(): Int {
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
internal fun Context.getNavigationBarHeight(): Int {
    return kotlin.runCatching {
        resources.getDimensionPixelSize(
            resources.getIdentifier("navigation_bar_height", "dimen", "android")
        )
    }.getOrNull() ?: 0
}
