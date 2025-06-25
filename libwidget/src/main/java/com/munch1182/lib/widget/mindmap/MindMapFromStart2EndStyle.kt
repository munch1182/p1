package com.munch1182.lib.widget.mindmap

import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.munch1182.lib.base.getTextRect
import com.munch1182.lib.base.log
import kotlin.math.max

object MindMapFromStart2EndStyle : MindMapView.NodeStyle {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val log = log()

    //要先测量高度，然后根据高度布局
    override fun layoutNode(node: MindMapView.Node): Array<MindMapView.NodeView> {
        // 测量所有节点的高度，父节点高度为子节点高度和
        measureNode(node, 0)
        val list = mutableListOf<MindMapView.NodeView>()
        layoutChildrenNode(list, 0, 0, node)
        list.reverse()
        return list.toTypedArray()
    }

    private fun measureNode(node: MindMapView.Node, level: Int): Float {
        val selfHeight = calculateNodeContent(node.name, level).height()
        val padding = nodePadding(level)
        if (node.children.isNullOrEmpty()) return selfHeight + padding.second

        var sum = 0f
        node.children?.forEachIndexed { index, it ->
            if (index > 0) sum += verticalPadding(level + 1)
            sum += measureNode(it, level + 1)
        }
        node.childrenHeight = sum
        return sum
    }

    private fun layoutChildrenNode(list: MutableList<MindMapView.NodeView>, level: Int, index: Int, currNode: MindMapView.Node, parent: MindMapView.NodeView? = null, last: MindMapView.NodeView? = null): MindMapView.NodeView {
        val contentRect = calculateNodeContent(currNode.name, level)
        val spaceRect = RectF(0f, 0f, contentRect.width(), if (currNode.childrenHeight == 0f) contentRect.height() else currNode.childrenHeight)
        val innerPadding = nodeContentPadding(level)
        setupTextPaint(level)
        val node = MindMapView.NodeView(
            currNode.name, level, spaceRect, contentRect,
            hContentPadding = innerPadding.first, vContentPadding = innerPadding.second,
            textSize = paint.textSize,
            fromID = parent?.id ?: "",
            id = MindMapIdHelper.newID(parent?.id, index)
        )
        adjustNodeViewByCommon(node)
        adjustNodeViewByLocation(node, parent, last)

        node.newLinkPoint(parent)

        var lastNode: MindMapView.NodeView? = null
        currNode.children?.forEachIndexed { i, it ->
            lastNode = layoutChildrenNode(list, level + 1, i, it, node, lastNode)
        }

        list.add(node)
        return node
    }

    private fun adjustNodeViewByLocation(node: MindMapView.NodeView, parent: MindMapView.NodeView?, last: MindMapView.NodeView?) {
        var xOffset = 0f
        var yOffset = 0f

        if (parent != null) xOffset += parent.contentRect.right + horizontalPadding(node.level)

        if (last == null && parent != null) {
            yOffset += parent.spaceRect.top
        } else if (last != null) {
            yOffset += last.spaceRect.bottom + verticalPadding(node.level)
        }

        node.spaceRect.offset(xOffset, yOffset)

        val height = node.contentRect.height()
        // 因为spaceRect已经加上padding，所以不需要再处理
        node.contentRect.left = node.spaceRect.left
        node.contentRect.right = node.spaceRect.right
        node.contentRect.top = node.spaceRect.top + (node.spaceRect.height() - height) / 2
        node.contentRect.bottom = node.contentRect.top + height
    }

    private fun verticalPadding(level: Int): Float {
        return max(100f - level * 5, 30f)
    }

    private fun horizontalPadding(level: Int): Float {
        return max(150f - level * 10, 50f)
    }

    /**
     * 调整节点位置, 包括间距或者其它样式需要
     */
    private fun adjustNodeViewByCommon(node: MindMapView.NodeView) {
        val padding = nodePadding(node.level)
        node.spaceRect.right += padding.first
        node.spaceRect.bottom += padding.second
    }

    private fun nodePadding(level: Int): Pair<Float, Float> {
        return 10f to 5f
    }

    private fun nodeContentPadding(level: Int): Pair<Int, Int> {
        return 10 to 3
    }

    private fun calculateNodeContent(node: String, level: Int): RectF {
        setupTextPaint(level)
        val rect = paint.getTextRect(node)
        val innerPadding = nodeContentPadding(level)
        // 加上内间距
        rect.right += innerPadding.first * 2f
        rect.bottom += innerPadding.second * 2f
        return rect
    }

    private fun setupTextPaint(level: Int) {
        paint.color = Color.BLACK
        paint.textSize = 36f
        paint.style = Paint.Style.FILL
    }
}