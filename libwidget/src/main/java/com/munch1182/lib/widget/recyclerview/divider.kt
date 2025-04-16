package com.munch1182.lib.widget.recyclerview

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.RecyclerView

class RecyclerViewDividerItemDecoration(
    private val dividerHeight: Int = 1,
    @ColorInt private val color: Int = Color.GRAY,
    private val paddingStart: Int = 0,
    private val paddingEnd: Int = 0,
) : RecyclerView.ItemDecoration() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { setColor(this@RecyclerViewDividerItemDecoration.color) }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDraw(c, parent, state)

        val left = parent.paddingLeft + paddingStart
        val right = parent.width - parent.paddingRight - paddingEnd
        val count = parent.childCount
        for (i in 0 until count) {
            val child = parent.getChildAt(i)
            val params = child.layoutParams as RecyclerView.LayoutParams

            val top = child.bottom + params.bottomMargin
            val bottom = top + dividerHeight

            c.drawRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), paint)
        }
    }
}