package com.munch1182.lib.view

import android.graphics.Canvas
import android.view.View
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.max

class ClickItemTouchHelper(private val rv: RecyclerView) :
    ItemTouchHelper(SlideItemHelperCallback(rv)) {

    init {
        attachToRecyclerView(rv)
    }
}

class SlideItemHelperCallback(private val rv: RecyclerView) : ItemTouchHelper.Callback() {
    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        return makeMovementFlags(0, ItemTouchHelper.LEFT)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ) = false

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            val maxW = viewHolder.menuView?.measuredWidth ?: return
            val clampedDX = max(dX, -maxW.toFloat())
            viewHolder.itemView.translationX = clampedDX

            val isOpen = -clampedDX >= maxW * 0.4f
            viewHolder.setIsMenuOpen(isOpen)
        }
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        // super.clearView(recyclerView, viewHolder)
    }
}

fun RecyclerView.ViewHolder.setIsMenuOpen(isOpen: Boolean) {
    itemView.tag = isOpen
}

val RecyclerView.ViewHolder.isMenuOpen: Boolean
    get() = itemView.tag as? Boolean ?: false

val RecyclerView.ViewHolder.menuLayout: SlideMenuLayout?
    get() = itemView as? SlideMenuLayout

val RecyclerView.ViewHolder.menuView: View?
    get() = menuLayout?.menuView