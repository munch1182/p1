package com.munch1182.lib.widget.mindmap

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.core.graphics.withMatrix
import com.munch1182.lib.base.log
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class MindMapView @JvmOverloads constructor(
    ctx: Context, set: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0
) : View(ctx, set, defStyleAttr, defStyleRes) {

    private val log = log()
    private var currStyle: NodeStyle = MindMapFromStart2EndStyle
    private val matrix = Matrix()

    private var currScale = 1f
    private val minScale = 1.0f
    private val maxScale = 5.0f
    private var currMode: Mode = Mode.Center

    // 记录的内容大小(未缩放的大小)
    private val contentRect = RectF()

    // 当前数据
    private var currNode: Node? = null
    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            matrix { centerContent() }
            return true
        }

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            matrix { postTranslate(-distanceX, -distanceY) }
            return true
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {

            return true
        }
    }
    private val scaleListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        private val point = PointF()
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            point.set(detector.focusX, detector.focusY)
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scale = detector.scaleFactor
            handleScale(scale, point.x, point.y)
            return true
        }
    }
    private val gestureDetector = GestureDetector(ctx, gestureListener)
    private val scaleDetector = ScaleGestureDetector(ctx, scaleListener)

    fun setNode(node: Node) {
        this.currNode = node
    }

    fun setStyle(style: NodeStyle) {
        this.currStyle = style
    }

    fun update(any: MindMapView.() -> Unit) {
        any(this)
        invalidate()
    }

    private inline fun matrix(any: Matrix.() -> Unit) {
        any(matrix)
        invalidate()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        currMode = Mode.Center
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        log.logStr("${event.actionMasked}")
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> currMode = Mode.Drag
            MotionEvent.ACTION_POINTER_DOWN -> currMode = Mode.Scale
        }
        when (currMode) {
            Mode.Drag -> gestureDetector.onTouchEvent(event)
            Mode.Scale -> scaleDetector.onTouchEvent(event)
            Mode.Center -> {}
        }
        return true
    }

    /**
     * 将内容居中, 如果内容比页面大，则居中并缩小；否则，居中并放大
     */
    private fun Matrix.centerContent() {
        postTranslate(abs(contentRect.width() - width) / 2f, abs(contentRect.height() - height) / 2f)
        val scale = max(contentRect.width() / width, contentRect.height() / height)

        log.logStr("scale: $scale")
        postScale(scale, scale, width / 2f, height / 2f)
    }

    /**
     * 处理缩放
     *
     * @param scale 缩放比例
     * @param x,y 缩放中心点
     */
    private fun handleScale(scale: Float, x: Float, y: Float) {
        log.logStr("handleScale: $scale, $x, $y")
        matrix {
            postScale(scale, scale, x, y)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val node = currNode ?: return
        var maxEnd = 0f
        var maxHeight = 0f
        canvas.withMatrix(matrix) {
            val views = currStyle.layoutNode(node)
            // 因为已经计算过子节点所占高度，所以整体高度即第一个节点的高度
            maxHeight = views[0].spaceRect.height()
            views.forEach {
                currStyle.drawNode(this, it)
                // 宽度即最右侧节点的右侧位置
                maxEnd = max(maxEnd, it.spaceRect.right)
            }
            contentRect.set(0f, 0f, maxHeight, maxEnd)/*if (currMode == Mode.Center) {
                this@MindMapView.matrix.centerContent()
            }*/
        }
    }

    open class NodeView(
        val name: String, // 标题
        val level: Int, // 层级，大多数样式都跟层级有关
        val spaceRect: RectF, // 节点占用位置
        val contentRect: RectF, // 节点显示区域
        var linkPoint: LinkPoint? = null // 子节点到其父节点的连接点
    )

    open class LinkPoint(open val sX: Float, open val sY: Float, open val eX: Float, open val eY: Float) {
        private val path = Path()

        open fun drawLink(canvas: Canvas, paint: Paint, level: Int) {
            if (abs(sY - eY) < 5f) {
                canvas.drawLine(sX, sY, eX, sY, paint)
                return
            }

            /*val sX = min(sX, eX)
            val eX = max(this.sX, eX)
            val sY = min(sY, eY)
            val eY = max(this.sY, eY)*/

            val disX = abs(eX - sX)
            val disY = abs(eY - sY)

            val dis = min(disX, disY)
            // 因为是最短的距离
            path.reset()


            val offsetStartX = dis * 0.1f
            val tbOffset = if (sY > eY) -1f else 1f

            val sc1x = sX + offsetStartX * 1.5f - disY * 0.01f
            val sc1y = sY + offsetStartX * 0.3f * tbOffset
            val sc2x = sX + offsetStartX * 3f + disY * 0.01f
            val sc2y = sY + offsetStartX * 2f * tbOffset
            val cX = sX + (eX - sX) / 3f + offsetStartX
            val cY = sY + (eY - sY) / 3f + offsetStartX * tbOffset
            path.moveTo(sX, sY)
            path.cubicTo(sc1x, sc1y, sc2x, sc2y, cX, cY)

            var offsetEnd = dis * 0.1f
            offsetEnd *= 3f
            val ec1x = eX - offsetEnd
            val ec1y = eY
            val ec2x = eX - offsetEnd - offsetEnd / 2f
            val ec2y = eY
            path.cubicTo(ec1x, ec1y, ec2x, ec2y, eX, eY)
            canvas.drawPath(path, paint)

            /*paint.setColor(Color.BLACK)
            canvas.drawCircle(sc1x, sc1y, 1f, paint)
            canvas.drawCircle(sc2x, sc2y, 1f, paint)
            canvas.drawCircle(ec1x, ec1y, 1f, paint)
            canvas.drawCircle(ec2x, ec2y, 1f, paint)
            paint.setColor(Color.CYAN)
            canvas.drawCircle(cX, cY, 1f, paint)*/
        }
    }

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

    private sealed class Mode {
        data object Drag : Mode()
        data object Scale : Mode()
        data object Center : Mode()
    }
}