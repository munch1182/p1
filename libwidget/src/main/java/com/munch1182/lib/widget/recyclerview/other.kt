package com.munch1182.lib.widget.recyclerview

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect

fun RecyclerViewTitleItemDecoration.TitleDrawHelper.newAsStickyHeader(): RecyclerViewStickyHearItemDecoration.StickyHearDrawHelper {
    return object : RecyclerViewStickyHearItemDecoration.StickyHearDrawHelper {
        override fun isTitle(pos: Int) = this@newAsStickyHeader.isTitle(pos)
        override fun headerHeight() = this@newAsStickyHeader.titleHeight()
        override fun onDraw(c: Canvas, rect: Rect, paint: Paint, nearestPos: Int) {
            this@newAsStickyHeader.onDraw(c, rect, paint, nearestPos)
        }
    }
}