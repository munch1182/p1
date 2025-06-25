package com.munch1182.lib.widget.mindmap

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ViewGroup
import androidx.core.graphics.contains
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withMatrix
import androidx.core.view.children
import com.munch1182.lib.base.dp2PX
import com.munch1182.lib.base.log
import com.munch1182.lib.base.mapByMatrix
import com.munch1182.lib.base.specSize
import com.munch1182.lib.base.withUI
import com.munch1182.lib.helper.SoftKeyBoardHelper
import java.io.File
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

class MindMapView @JvmOverloads constructor(
    ctx: Context, set: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0
) : ViewGroup(ctx, set, defStyleAttr, defStyleRes) {

    private val log = log()
    private var currStyle: NodeStyle = MindMapFromStart2EndStyle
    private val matrix = Matrix()

    init {
        SoftKeyBoardHelper.enableEditMode(this)
        setWillNotDraw(false)
    }

    private var currScale = 1f
    private val minScale = 1.0f
    private val maxScale = 5.0f
    private var currMode: Mode = Mode.Center

    val currScaleValue get() = currScale

    // 记录的内容大小(未缩放的大小)
    private val contentRect = RectF()

    // 当前使用的节点位置
    private var nodeViews: Array<NodeView>? = null

    // 当前数据
    private var currNode: Node? = null

    // 当前选中的节点的位置(nodeViews)
    private var currSelectIndex: Int = -1

    override fun getMatrix(): Matrix {
        return matrix
    }

    // 按键回调
    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            judgeSelect(e) { _, node -> selectNode(node) }
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            super.onLongPress(e)
            judgeSelect(e) { _, node -> selectNode(node, true) }
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            matrix { centerContent() }
            return true
        }

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            if (currSelectIndex != -1) {
                noSelectEdit()
            }
            matrix { postTranslate(-distanceX, -distanceY) }
            return true
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            return true
        }
    }

    // 缩放回调
    private val scaleListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        private val point = PointF()
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            point.set(detector.focusX, detector.focusY)
            nodeViews = null
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scale = detector.scaleFactor
            currScale = scale
            handleScale(scale, point.x, point.y)
            return true
        }
    }
    private val gestureDetector = GestureDetector(ctx, gestureListener)
    private val scaleDetector = ScaleGestureDetector(ctx, scaleListener)

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        log.logStr("onKeyDown: $keyCode, ${event?.action}, ${event?.isPrintingKey}")
        when (keyCode) {
            KeyEvent.KEYCODE_ENTER -> {
                nodeViews?.getOrNull(currSelectIndex)?.commitText("\n")
                invalidate()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private val boardHelper by lazy {
        SoftKeyBoardHelper(this).setKeyBoardChangeListener {
            val isHideBoard = it <= 0
            if (isHideBoard) {
                noSelectEdit()
            }
            log.logStr("onBoardChange: $it")
        }
    }

    private fun noCurrSelectIndex() {
        currSelectIndex = -1
    }

    private fun selectNode(node: NodeView, isEdit: Boolean = false) {
        if (currSelectIndex != -1 && currSelectIndex != node.currIndex) {
            nodeViews?.getOrNull(currSelectIndex)?.noSelect()
        }
        currSelectIndex = node.currIndex
        node.select(isEdit)
        adjustSelectNode(node)
        if (isEdit) {
            showInput(node)
        } else {
            showMenu(node)
        }
    }

    fun startEditByNode(node: NodeView) {
        selectNode(node, true)
    }

    fun setNode(node: Node) {
        this.currNode = node
        reset()
    }

    fun setStyle(style: NodeStyle) {
        this.currStyle = style
    }

    fun update(any: MindMapView.() -> Unit) {
        any(this)
        invalidate()
    }

    private fun reset() {
        this.nodeViews = null
        this.contentRect.set(0f, 0f, 0f, 0f)
        this.currMode = Mode.Center
    }

    private inline fun matrix(any: Matrix.() -> Unit) {
        noSelectEdit()
        any(matrix)
        invalidate()
    }

    private fun noSelectEdit() {
        if (SoftKeyBoardHelper.im.isActive) {
            SoftKeyBoardHelper.hide(this)
        }
        val nodeView = nodeViews?.getOrNull(currSelectIndex)
        noCurrSelectIndex()
        removeAllViews()
        nodeView?.let {
            it.noSelect()
            resetChildAsParentEditNot(it)
            invalidate()
        }
    }

    fun newTextScale(): Float {
        val vector = floatArrayOf(0f, 1f)
        val mappedVector = FloatArray(2)
        matrix.mapVectors(mappedVector, vector)
        val scale = hypot(mappedVector[0], mappedVector[1])
        return scale
    }

    fun newWidthScale(): Float {
        val vector = floatArrayOf(1f, 0f)
        val mappedVector = FloatArray(2)
        matrix.mapVectors(mappedVector, vector)
        val scale = hypot(mappedVector[0], mappedVector[1])
        return scale
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(widthMeasureSpec.specSize, heightMeasureSpec.specSize)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        //currMode = Mode.Center
        children.forEach {
            val lp = it.layoutParams as? MarginLayoutParams
            val l = lp?.leftMargin ?: 0
            val t = lp?.topMargin ?: 0
            val r = (lp?.width ?: 0) + l
            val b = (lp?.height ?: 0) + t
            it.layout(l, t, r, b)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> currMode = Mode.Drag
            MotionEvent.ACTION_POINTER_DOWN -> {
                currMode = Mode.Scale
                // 因为gestureDetector的长按是延时判断，所以当多指按下时需要传入事件以取消长按判断
                gestureDetector.onTouchEvent(event)
            }
        }
        when (currMode) {
            Mode.Drag -> gestureDetector.onTouchEvent(event)
            Mode.Scale -> scaleDetector.onTouchEvent(event)
            Mode.Center -> {}
        }
        return true
    }

    fun setEditMode() {
    }

    /**
     * 将内容居中, 如果内容比页面大，则居中并缩小；否则，居中并放大
     */
    private fun Matrix.centerContent() {
        val w = width - innerPadding * 2f
        val h = height - innerPadding * 2f
        val scale = min(w / contentRect.width(), h / contentRect.height())
        log.logStr("scale: $scale")
        reset()
        postTranslate((w - contentRect.width() * scale) / 2 + innerPadding, (h - contentRect.height() * scale) / 2 + innerPadding)
        currScale = scale
        postScale(scale, scale, 0f, 0f)
    }

    private val innerPadding get() = 16.dp2PX

    /**
     * 处理缩放
     *
     * @param scale 缩放比例
     * @param x,y 缩放中心点
     */
    private fun handleScale(scale: Float, x: Float, y: Float) {
        matrix { postScale(scale, scale, x, y) }
    }

    /**
     * 将内容写入到一个bitmap中，以供诸如分享之内的操作
     */
    suspend fun withBitmap(any: (Bitmap) -> Unit) {
        nodeViews?.find { it.isSelected }?.isSelected = false // 重置选择状态
        withUI { matrix { centerContent() } } // 分享时先居中，否则只能分享成缩放后的样子
        this.post { // 等待居中绘制完成
            val bitmap = createBitmap(width * 2, height * 2) // 放大两倍，防止模糊
            val canvas = Canvas(bitmap)
            val matrix = Matrix()
            matrix.postScale(2f, 2f)
            canvas.withMatrix(matrix) { draw(canvas) }
            any(bitmap)
            bitmap.recycle()
        }
    }

    /**
     * 将内容写入到一个bitmap中，然后再转成文件
     *
     * @param file 要写入的文件
     * @param any 写入操作完成回调
     *
     * @see withBitmap
     */
    suspend fun withBitmap2File(file: File, any: (File) -> Unit) {
        withBitmap { bt ->
            file.outputStream().use {
                try {
                    bt.compress(Bitmap.CompressFormat.PNG, 100, it)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            any(file)
        }
    }

    /**
     * 当一个节点被选中时，调整相关视图
     *
     * 1. 将该节点缩放(如果需要)并居中
     * 2. 设置该节点的editRect的范围
     */
    private fun adjustSelectNode(node: NodeView?) {
        val content = node?.contentRealRect ?: return

        // 只用于调整位置，因为进过转换，所以不能实际使用
        val rect = content.mapByMatrix(matrix)
        //rect.right += node.wPadding * 2

        val wp = node.wPadding
        val containerRect = RectF(wp, wp, width.toFloat() - wp, height.toFloat() - wp)
        val offX = containerRect.centerX() - rect.centerX()
        val offY = containerRect.centerY() - rect.centerY()
        matrix.postTranslate(offX, offY)

        val scale = containerRect.width() / rect.width()
        log.logStr("adjustCenter4SelectRect: $scale")
        // 只缩小不放大
        if (scale < 1) {
            matrix.postScale(scale, scale, containerRect.centerX(), containerRect.centerY())
            currScale = scale
        }
    }

    private fun showMenu(node: NodeView?) {
        log.logStr("showInput: ${node?.name}")
        node ?: return
        removeAllViews()
        val menu = MindMapMenuView.newNode(this, node)
        addView(menu)
    }

    private fun showInput(node: NodeView?) {
        log.logStr("showInput: ${node?.name}")
        node ?: return
        removeAllViews()
        node.editRectF = RectF(node.contentRealRect)
        val et = MindMapEditView.newNode(this, node)
        addView(et)
        post { SoftKeyBoardHelper.show(et) }
    }

    override fun generateDefaultLayoutParams(): LayoutParams {
        return MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        boardHelper.listen()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        boardHelper.unListen()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val node = currNode ?: return
        // 有限使用缓存数据，因为要处理选中效果。因此当数据更改时，需要清空缓存
        if (nodeViews == null) {
            val views = currStyle.layoutNode(node)
            nodeViews = views
        }
        val views = nodeViews ?: return
        // 如果要居中，需要计算内容宽高
        if (currMode.isCenter) {
            if (contentRect.width() == 0f && contentRect.height() == 0f) {
                // 因为已经计算过子节点所占高度，所以整体高度即第一个节点的高度
                val maxHeight = views[0].spaceRect.height()
                var maxEnd = 0f
                views.forEach {
                    // 宽度即最右侧节点的右侧位置
                    maxEnd = max(maxEnd, it.spaceRect.right)
                }
                contentRect.set(0f, 0f, maxEnd, maxHeight)
            }
            this@MindMapView.matrix.centerContent()
        }
        canvas.withMatrix(matrix) {
            views.forEachIndexed { index, it ->
                it.currIndex = index
                currStyle.drawNode(this, it)
            }
        }
    }

    /**
     * 当父节点进行编辑导致变宽时，子节点需要跟着移动
     */
    fun updateChildAsParentEdit(node: NodeView) {
        val nodes = nodeViews ?: return
        val editRect = node.editRectF ?: return
        val offset = editRect.right - node.contentRealRect.right
        log.logStr("updateChildAsParentEdit offset: $offset")
        if (offset <= 0) return
        nodes.filter { it.fromID == node.id }.forEach {
            it.editRectF = RectF(it.contentRealRect)
            it.editRectF?.offset(offset, 0f)
            it.newLinkPoint(node)
            updateChildAsParentEdit(it)
        }
    }

    /**
     * 当父节点退出编辑时，恢复相关子节点
     */
    fun resetChildAsParentEditNot(node: NodeView) {
        val nodes = nodeViews ?: return
        node.noSelect()
        nodes.filter { it.fromID == node.id }.forEach {
            it.noSelect()
            it.newLinkPoint(node)
            resetChildAsParentEditNot(it)
        }
    }

    /**
     * 传入按键事件，判断选中； 只判断，不处理
     */
    private fun judgeSelect(e: MotionEvent, select: (Int, NodeView) -> Unit) {
        val nodes = nodeViews ?: return

        val longPressPointF = PointF()
        longPressPointF.set(e.x, e.y)

        // 缓存不如实时计算有效率
        val mappedRect = MutableList(nodes.size) { nodes[it].contentRect.mapByMatrix(matrix) }

        mappedRect.forEachIndexed { index, it ->
            val isSelected = it.contains(longPressPointF)
            if (isSelected) {
                val nodeView = nodes[index]
                log.logStr("isSelected: ${nodeView.name}, $it, $longPressPointF")
                select(index, nodeView)
                return@forEachIndexed
            }
        }
    }

    open class NodeView(
        val name: String, // 标题
        val level: Int, // 层级，大多数样式都跟层级有关
        val spaceRect: RectF, // 节点占用位置
        val contentRealRect: RectF, // 节点显示区域
        var contentLinkPoint: LinkPoint? = null, // 子节点到其父节点的连接点

        val hContentPadding: Int = 0, // 水平内容间距
        val vContentPadding: Int = 0, // 垂直内容间距
        // 选中相关
        var isSelected: Boolean = false, // 预留菜单选择
        var isEditSelected: Boolean = false, // 节点编辑模式
        var textSize: Float = 36f, // 文字大小
        var editRectF: RectF? = null, // 编辑区域，用于缓存诸如增加的区域
        var editLinkPoint: LinkPoint? = null, // 编辑模式下，子节点到其父节点的连接点

        val id: String = "", // 节点id，
        val fromID: String = "", // 父节点id
    ) {

        val contentRect get() = editRectF ?: contentRealRect
        val linkPoint get() = editLinkPoint ?: contentLinkPoint

        /**
         * 节点在nodeviews中的索引，有时效，且需要实时更新
         */
        internal var currIndex: Int = -1

        /**
         * 编辑节点
         */
        fun commitText(toString: String) {
            if (!isEditSelected) return
        }

        fun noSelect() {
            isEditSelected = false
            isSelected = false
            editRectF = null
            editLinkPoint = null
        }

        fun select(isEdit: Boolean = false) {
            isSelected = true
            isEditSelected = isEdit
        }

        /**
         * 根据当前属性和父节点生成连接点
         */
        fun newLinkPoint(parent: NodeView?) {
            parent ?: return
            val newLP = LinkPoint(parent.contentRect.right, parent.contentRect.centerY(), contentRect.left, contentRect.centerY())
            if (isEditSelected) {
                editLinkPoint = newLP
            } else {
                contentLinkPoint = newLP
            }
        }


        // 节点圆角
        val radius get() = contentRect.height() / 4f

        // 节点水平上间距
        val wPadding get() = min(contentRect.width() * 0.03f, 10f)
    }

    open class LinkPoint(open val sX: Float, open val sY: Float, open val eX: Float, open val eY: Float) {

        open fun drawLink(canvas: Canvas, paint: Paint, level: Int) {
            MindMapCommon.drawLink(canvas, paint, level, this)
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

        /**
         * 实际绘制节点，会将layoutNode返回的NodeView作为参数无序传入
         * 已处理缩放、移动、选择等通用项，只需要按照node的参数绘制即可
         */
        fun drawNode(canvas: Canvas, node: NodeView) {
            MindMapCommon.drawNode(canvas, node)
        }
    }

    private sealed class Mode {
        data object Drag : Mode()
        data object Scale : Mode()
        data object Center : Mode()

        val isCenter get() = this is Center
    }
}