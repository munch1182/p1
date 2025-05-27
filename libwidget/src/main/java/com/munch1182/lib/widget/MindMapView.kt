package com.munch1182.lib.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.munch1182.lib.base.toRectF

class MindMapView @JvmOverloads constructor(
    ctx: Context, set: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0
) : View(ctx, set, defStyleAttr, defStyleRes) {

    class Node(val name: String, val children: Array<Node>?)

    class NodeView(
        val node: String, // 节点名称
        val nodeVal: NodeVal, // 节点位置+是否是强调节点，影响当前节点的绘制样式，包括边框、背景、文字颜色等
        val rect: RectF? = null // 当前节点大小，已经测量过文字摆放，包含放置的位置，放置的位置还受节点位置和样式影响(左右前后)
    )

    data class NodeVal(
        val nodePos: Int, // node位置, 例如，0代表父节点，1102代表从父节点开始，父节点的第二个子节点下的第二个子节点下的第一个子节点下的第三个子节点(从0开始)
        val isEmphasis: Boolean = false, // 是否是强调节点
    )

    // 不同的样式
    interface NodeStyle {
        // 根据样式摆放节点，所有的节点都由第一个节点而来
        // center: 画布原点
        fun layoutNodes(center: PointF, node: Node): Array<NodeView>

        // 绘制统一层级的节点 @see drawNode
        fun drawNodes(canvas: Canvas, node: Array<NodeView>)

        // 绘制单个节点
        fun drawNode(canvas: Canvas, node: NodeView)

        // 绘制父节点到所有子节点的连接线
        fun drawLinkLine(canvas: Canvas, parent: NodeView, children: Array<NodeView>)
    }

    abstract class BaseNodeStyle : NodeStyle {
        protected open val paint = Paint()

        /**
         * 调整node的大小，主要是padding+border
         * 调整会更改{@see rect}的值
         *
         * @param rect node文字的大小，如果要折叠文字，是折叠后的大小
         */
        protected open fun adjustNodeRectWith(rect: RectF?, node: NodeVal) {
            rect?.inset(12f, 12f)
        }

        protected open fun setFontPaint() {}
    }

    // 父节点在左侧，从左往右
    class FromStart2RightNodeStyle : BaseNodeStyle() {

        private fun layoutNodes(center: PointF, currNode: Node, pos: Int, list: MutableList<NodeView>) {
            currNode.children?.forEachIndexed { index, node ->
                val nodePos = pos * 10 + index // 不超过10位数
                val nodeVal = NodeVal(nodePos)
                list.add(NodeView(node.name, nodeVal, layoutNodeRect(node.name, nodeVal)))
                layoutNodes(center, node, nodePos, list)
            }
        }

        private fun layoutLinkPoint(rect: RectF?) {

        }

        override fun adjustNodeRectWith(rect: RectF?, node: NodeVal) {
            super.adjustNodeRectWith(rect, node)
        }

        override fun layoutNodes(center: PointF, node: Node): Array<NodeView> {
            val list = mutableListOf<NodeView>()
            val nodeVal = NodeVal(0)
            list.add(NodeView(node.name, nodeVal, layoutNodeRect(node.name, nodeVal)))
            layoutNodes(center, node, 0, list)
            return list.toTypedArray()
        }

        private fun layoutNodeRect(name: String, nodeVal: NodeVal): RectF {
            setFontPaint()
            val rect = Rect()
            var finalName = name
            if (name.length > 15) finalName = name.substring(0, 15) + "..."
            paint.getTextBounds(finalName, 0, finalName.length, rect)
            val rectF = rect.toRectF()
            adjustNodeRectWith(rectF, nodeVal)
            return rectF
        }

        override fun drawNodes(canvas: Canvas, node: Array<NodeView>) {
        }

        override fun drawNode(canvas: Canvas, node: NodeView) {
        }

        override fun drawLinkLine(canvas: Canvas, parent: NodeView, children: Array<NodeView>) {
        }
    }

    val DefaultNodeStyle = FromStart2RightNodeStyle()
}