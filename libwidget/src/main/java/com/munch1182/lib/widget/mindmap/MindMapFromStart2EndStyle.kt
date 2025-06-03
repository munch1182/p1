package com.munch1182.lib.widget.mindmap

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF

object MindMapFromStart2EndStyle : MindMapView.NodeStyle {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    //要先测量高度，然后根据高度布局
    override fun layoutNode(node: MindMapView.Node): Array<MindMapView.NodeView> {
        measureNodeHeight(node)
        // 测量所有节点的高度，父节点高度为子节点高度和
        val list = mutableListOf<MindMapView.NodeView>()
        layoutNodeChildren(list, 0, node)
        return list.toTypedArray()
    }

    private fun measureNodeHeight(node: MindMapView.Node): Float {
        val selfHeight = calculateSelfHeight(node)

        if (node.children.isNullOrEmpty()) {
            node.totalHeight = selfHeight
            return selfHeight
        }

        var totalHeight = 0f
        node.children.forEachIndexed { index, child ->
            totalHeight += measureNodeHeight(child)
            if (index > 0) totalHeight += spaceByLevel()
        }

        // 父节点采取的高度是顶部到文字底部的高度，而不是完整的高度
        val height = totalHeight / 2f + selfHeight / 2f
        node.totalHeight = height

        return height
    }

    private fun spaceByLevel(): Float {
        return 10f
    }

    private fun calculateSelfHeight(node: MindMapView.Node): Float {
        return 40f
    }

    private fun layoutNodeChildren(result: MutableList<MindMapView.NodeView>, level: Int, curr: MindMapView.Node, parent: MindMapView.NodeView? = null) {
        val rect = newRectFromParent(parent)
        val nodeView = MindMapView.NodeView(curr.name, level, rect)
        result.add(nodeView)
        curr.children?.forEach { child ->
            layoutNodeChildren(result, level + 1, child, nodeView)
        }
    }

    private fun newRectFromParent(parent: MindMapView.NodeView?): RectF {
        return parent?.let { RectF(it.contentRect.right + spaceByLevel(), 0f, 0f, 0f) } ?: RectF()
    }

    override fun drawNode(canvas: Canvas, node: MindMapView.NodeView) {
    }

    override fun drawLinkLine(canvas: Canvas, from: MindMapView.NodeView, to: MindMapView.NodeView) {
    }

}