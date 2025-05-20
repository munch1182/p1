package com.munch1182.lib.widget.recyclerview

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView


class RecyclerViewTitleItemDecoration(private val helper: TitleDrawHelper) : RecyclerView.ItemDecoration() {

    interface TitleDrawHelper {
        fun isTitle(pos: Int): Boolean
        fun titleHeight(): Int
        fun onDraw(c: Canvas, rect: Rect, paint: Paint, pos: Int)
        fun posHeight(pos: Int) = if (isTitle(pos)) titleHeight() else 0
    }

    private val p by lazy { Paint(Paint.ANTI_ALIAS_FLAG) }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        super.getItemOffsets(outRect, view, parent, state)
        val pos = parent.getChildAdapterPosition(view)
        outRect.set(0, helper.posHeight(pos), 0, 0)
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDraw(c, parent, state)
        val left = parent.paddingStart
        val right = parent.width - parent.paddingEnd
        val count = parent.childCount
        val rect = Rect()
        for (i in 0 until count) {
            val child = parent.getChildAt(i)
            val pos = parent.getChildAdapterPosition(child)
            if (!helper.isTitle(pos)) {
                continue
            }
            val bottom = child.top // 因为getItemOffsets已经留足位置
            val top = bottom - helper.titleHeight()

            rect.set(left, top, right, bottom)
            drawTitle(c, rect, pos)
        }
    }

    private fun drawTitle(c: Canvas, rect: Rect, pos: Int) {
        helper.onDraw(c, rect, p, pos)
    }
}