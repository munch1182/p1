package com.munch1182.lib.floatwindow

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.PointF
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.munch1182.lib.floatwindow.FWManager.isTagView

class FWWidget(
    private val view: View,
    private val lp: WindowManager.LayoutParams = WindowManager.LayoutParams()
) : IFWComponent {

    private val appCtx: Context = view.context.applicationContext
    private val onTouchListener by lazy {
        OnTouchListener { x, y ->
            lp.x += x
            lp.y += y
            wm()?.updateViewLayout(view, lp)
        }
    }

    private fun wm(): WindowManager? {
        return kotlin.runCatching { appCtx.getSystemService(Context.WINDOW_SERVICE) as? WindowManager }
            .getOrNull()
    }

    override fun create(): Boolean {
        if (view.isAttachedToWindow) {
            return view.isTagView()
        }
        view.setTag(FWManager.tagKey, FWManager.tag)
        updateLP()
        hide()
        kotlin.runCatching { wm()?.addView(view, lp) }.getOrNull() ?: return false
        view.setOnTouchListener(onTouchListener)
        return true
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

    override fun show() {
        view.visibility = View.VISIBLE
    }

    override fun hide() {
        view.visibility = View.INVISIBLE
    }

    override fun destroy() {
        view.setTag(FWManager.tagKey, null)
        view.setOnTouchListener(null)
        if (view.isAttachedToWindow) {
            kotlin.runCatching { wm()?.removeView(view) }
        }
    }

    fun setOnClickLister(onClick: View.OnClickListener) {
        view.setOnClickListener(onClick)
    }

    class OnTouchListener(private val update: (x: Int, y: Int) -> Unit) : View.OnTouchListener {

        private val startPoint = PointF()
        private val lastMovePoint = PointF()
        private var isMoving = false

        override fun onTouch(v: View?, event: MotionEvent?): Boolean {
            v ?: return false
            event ?: return false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startPoint.set(event.rawX, event.rawY)
                    lastMovePoint.set(startPoint)
                }

                MotionEvent.ACTION_MOVE -> {
                    isMoving = true
                    val x = event.rawX - lastMovePoint.x
                    val y = event.rawY - lastMovePoint.y
                    lastMovePoint.set(event.rawX, event.rawY)
                    update(x.toInt(), y.toInt())
                }

                MotionEvent.ACTION_UP -> {
                    if (!isMoving) {
                        v.performClick()
                        return false
                    }
                    startPoint.reset()
                    lastMovePoint.reset()
                    isMoving = false
                }
            }
            return true
        }

        private fun PointF.reset() {
            set(-1f, -1f)
        }

        private fun PointF.isUnset() = x == -1f && y == -1f

    }
}