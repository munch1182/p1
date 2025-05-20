package com.munch1182.lib.widget.recyclerview

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


/**
 * 只支持[LinearLayoutManager]布局
 *
 * 只是显示效果，如果要实现点击效果，则需要更改或者保存数据结构
 */
class RecyclerViewStickyHearItemDecoration(private val helper: StickyHearDrawHelper) : RecyclerView.ItemDecoration() {

    interface StickyHearDrawHelper {
        fun isTitle(pos: Int): Boolean
        fun headerHeight(): Int
        fun onDraw(c: Canvas, rect: Rect, paint: Paint, nearestPos: Int)
    }

    private val p by lazy { Paint(Paint.ANTI_ALIAS_FLAG) }

    private var pos = 0
    private var rect = Rect()
    private var updateWhenUp = false

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDrawOver(c, parent, state)
        val firstPos = (parent.layoutManager as? LinearLayoutManager)?.findFirstVisibleItemPosition() ?: return
        if (firstPos == RecyclerView.NO_POSITION) return
        val vh = parent.findViewHolderForAdapterPosition(firstPos)?.itemView ?: return

        var saved = false

        val headerHeight = helper.headerHeight()
        if (helper.isTitle(firstPos + 1)) {
            if (vh.height + vh.top < headerHeight) {
                c.save()
                saved = true
                val offset = vh.height + vh.top - headerHeight
                c.translate(0f, offset.toFloat())

                pos = firstPos
                updateWhenUp = false
            }
        } else if (!updateWhenUp) {
            pos = firstPos + 1
            updateWhenUp = true
        }

        val left = parent.paddingLeft
        val right = parent.width - parent.paddingRight
        val top = parent.paddingTop
        val bottom = top + headerHeight
        rect.set(left, top, right, bottom)
        helper.onDraw(c, rect, p, pos)

        if (saved) c.restore()
    }
}