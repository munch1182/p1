package com.munch1182.lib.floatwindow

import android.content.Context
import android.graphics.PixelFormat
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager

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

    fun hide(id: String = DEFAULT_ID) {
        map[id]?.hide()
    }

    fun destroy(id: String = DEFAULT_ID) {
        map[id]?.destroy() ?: return
        map.remove(id)
    }
}

class FWWidget internal constructor(private val view: View) {

    private val ctx = view.context.applicationContext
    private val lp by lazy { WindowManager.LayoutParams() }

    internal fun create(): FWWidget {
        if (view.isAttachedToWindow) {
            return this
        }
        view.id = R.id.flow_window
        updateLP()
        hide()
        view.setOnTouchListener(FWTouchListener())
        ctx.wm()?.addView(view, lp)
        return this
    }

    fun show() {
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

}

internal class FWTouchListener : View.OnTouchListener {
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        TODO("Not yet implemented")
    }
}

fun Context.wm(): WindowManager? {
    return kotlin.runCatching { getSystemService(Context.WINDOW_SERVICE) as? WindowManager }
        .getOrNull()
}