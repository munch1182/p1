package com.munch1182.lib.widget.mindmap

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Matrix
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.toColorInt
import com.munch1182.lib.base.mapByMatrix
import com.munch1182.lib.base.newCornerDrawable

@SuppressLint("ViewConstructor")
class MindMapMenuView(private val mp: MindMapView, private val node: MindMapView.NodeView) : LinearLayout(mp.context) {

    private val ops = arrayOf("编辑", "添加节点", "添加子节点", "删除")

    init {
        translationZ = 16f
        background = newCornerDrawable(strokeColor = "#F3F3F3".toColorInt()).apply { setColor(Color.WHITE) }
        elevation = 12f
        gravity = Gravity.CENTER_VERTICAL

        ops.forEachIndexed { i, it ->
            if (i > 0) {
                addView(View(context).apply {
                    layoutParams = LayoutParams(1, LayoutParams.MATCH_PARENT)
                    setBackgroundColor(Color.GRAY)
                })
            }
            addView(TextView(context).apply {
                text = it
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setTextColor(Color.BLACK)
                setPadding(10, 5, 10, 5)
                setOnClickListener { menu(i) }
            })
        }
    }

    private fun menu(i: Int) {
        when (i) {
            0 -> {
                mp.startEditByNode(node)
            }

            1 -> {
            }

            2 -> {

            }
        }
    }

    override fun getMatrix(): Matrix {
        return mp.matrix
    }

    companion object {
        fun newNode(mp: MindMapView, node: MindMapView.NodeView): MindMapMenuView {
            val menu = MindMapMenuView(mp, node)
            menu.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)

            val rect = node.contentRect.mapByMatrix(mp.matrix)
            val lp = MarginLayoutParams(menu.measuredWidth, menu.measuredHeight)
            menu.layoutParams = lp
            menu.translationX = rect.left
            menu.translationY = rect.top - lp.height - menu.space

            val xOffset = (lp.width - rect.width()) / 2f
            menu.translationX -= xOffset

            return menu
        }
    }

    private val space get() = measuredHeight * 0.3f
}