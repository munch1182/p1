package com.munch1182.lib.widget.mindmap

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.munch1182.lib.base.drawTextInCenter

class MindMapNode @JvmOverloads constructor(
    ctx: Context, set: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0
) : View(ctx, set, defStyleAttr, defStyleRes) {

    private val rect = RectF()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        rect.set(0f, 0f, width.toFloat(), height.toFloat())
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        setupTextPaint()
        canvas.drawTextInCenter("12123123", rect.centerX(), rect.centerY(), paint)
    }

    private fun setupTextPaint() {
        paint.textSize = 36f
        paint.color = Color.BLACK
    }
}