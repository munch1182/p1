package com.munch1182.lib.widget.mindmap

import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class MindMapView @JvmOverloads constructor(
    ctx: Context, set: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0
) : View(ctx, set, defStyleAttr, defStyleRes) {

    private var currStyle: NodeStyle = MindMapFromStart2EndStyle

    private var currNode = Node(
        "rootyi", arrayOf(
            Node(
                "child1", arrayOf(
                    Node("child11"), Node("chi112", arrayOf(Node("aaa"), Node("bbb"))), Node("child113", arrayOf(Node("11111"), Node("111222"))), Node("child115")
                )
            ), Node("child2", arrayOf(Node("child22"))), Node("child3")
        )
    )

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val views = currStyle.layoutNode(currNode)
        views.forEach { currStyle.drawNode(canvas, it) }
    }

    class NodeView(
        val name: String, // 标题
        val level: Int, // 层级，大多数样式都跟层级有关
        val spaceRect: RectF, // 节点占用位置
        val contentRect: RectF, // 节点显示区域
        val linkPoints: MutableList<Float> = MutableList(4) { 0f }
    )

    class Node(val name: String, val children: Array<Node>? = null) {
        internal var childrenHeight: Float = 0f // 子节点占用高度，用于计算父节点位置
    }

    interface NodeStyle {

        /**
         * 计算节点位置, 具体位置跟样式有关 (比如：文字大小，间距，是否左右摆放)
         * 父节点的位置还受子节点的数量有关，如果子节点过多，父节点要保证处于子节点居中的位置因此需要下移（同层级间距会因为子节点而不一致）
         *
         * 所以：
         * 布局子节点时，使用父节点的end位置确定子节点的start位置，使用子节点的文本及样式允许的最大宽度确定子节点的宽高，
         * 组合子节点显示高度的中点确定父节点位置（只与相邻节点的显示高度有关，与间隔一个节点的高度无关）
         */
        fun layoutNode(node: Node): Array<NodeView>
        fun drawNode(canvas: Canvas, node: NodeView)
    }
}