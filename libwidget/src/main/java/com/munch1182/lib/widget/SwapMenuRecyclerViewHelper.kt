package com.munch1182.lib.widget

import android.annotation.SuppressLint
import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.munch1182.lib.base.log

@SuppressLint("ClickableViewAccessibility")
class SwapMenuRecyclerViewHelper(rv: RecyclerView) {

    private val log = log()
    private val manager = SwapOpenManager()
    private val lm = DisVerticalScrollLinearLayoutManager(rv.context)

    init {
        rv.layoutManager = lm
    }

    fun bind(swap: SwapMenuLayout, pos: Int) {
    }

    private class DisVerticalScrollLinearLayoutManager(ctx: Context) : LinearLayoutManager(ctx) {
        private var enable = true
        fun disable() {
            enable = false
        }

        fun allow() {
            enable = true
        }

        override fun canScrollVertically(): Boolean {
            return enable && super.canScrollVertically()
        }
    }

    private class SwapOpenManager {
        private var curr: SwapMenuLayout? = null

        val isAnyOpen: Boolean get() = curr?.isOpen ?: false

        fun open(layout: SwapMenuLayout) {
            curr?.close()
            curr = layout
            curr?.open()
        }

        fun close() {
            curr?.close()
            curr = null
        }
    }
}