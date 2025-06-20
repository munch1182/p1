package com.munch1182.lib.widget.mindmap

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.os.Bundle
import android.text.InputType
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.CompletionInfo
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputContentInfo
import android.view.inputmethod.TextAttribute
import androidx.annotation.WorkerThread
import androidx.core.graphics.contains
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withMatrix
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.munch1182.lib.base.dp2PX
import com.munch1182.lib.base.launchIO
import com.munch1182.lib.base.log
import com.munch1182.lib.helper.SoftKeyBoardHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.min

class MindMapView @JvmOverloads constructor(
    ctx: Context, set: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0
) : View(ctx, set, defStyleAttr, defStyleRes) {

    private val log = log()
    private var currStyle: NodeStyle = MindMapFromStart2EndStyle
    private val matrix = Matrix()

    init {
        SoftKeyBoardHelper.enableEditMode(this)
    }

    private val minScale = 1.0f
    private val maxScale = 5.0f
    private var currMode: Mode = Mode.Center

    // 记录的内容大小(未缩放的大小)
    private val contentRect = RectF()

    // 当前使用的节点位置
    private var nodeViews: Array<NodeView>? = null

    // 当前数据
    private var currNode: Node? = null

    // 当前正在编辑的节点的位置(nodeViews)
    private var currEditIndex: Int = -1
    private val currCursorJob = CursorJob(this)

    // 必须在onAttachedToWindow()之后调用
    private val scope by lazy { findViewTreeLifecycleOwner()?.lifecycleScope }

    // 按键回调
    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onLongPress(e: MotionEvent) {
            super.onLongPress(e)
            val nodes = nodeViews ?: return

            val longPressPointF = PointF()
            longPressPointF.set(e.x, e.y)

            // 缓存不如实时计算有效率
            val mappedRect = MutableList(nodes.size) { RectF().apply { matrix.mapRect(this, nodes[it].contentRect) } }
            var anySelected = false

            nodes.getOrNull(currEditIndex)?.noSelect()
            noCurrEditIndex()
            currCursorJob.stopEditMode()

            mappedRect.forEachIndexed { index, it ->

                val isSelected = it.contains(longPressPointF)
                nodes[index].isSelected = isSelected
                nodes[index].isEditSelected = isSelected
                if (isSelected) {
                    anySelected = true
                    log.logStr("isSelected: ${nodes[index].name}, $it, $longPressPointF")
                    currEditIndex = index
                    return@forEachIndexed
                }

            }
            if (anySelected) {
                adjustSelectNode(nodes.getOrNull(currEditIndex))
                invalidate() // 编辑交由onDraw绘制
                post { showInput() }

                scope?.launchIO {
                    currCursorJob.startEditMode(nodes.getOrNull(currEditIndex))
                }
            }
        }

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
            handleScale(scale, point.x, point.y)
            return true
        }
    }
    private val gestureDetector = GestureDetector(ctx, gestureListener)
    private val scaleDetector = ScaleGestureDetector(ctx, scaleListener)

    // 键盘操作
    private val inputConn by lazy {
        object : BaseInputConnection(this, true) {
            override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
                log.logStr("commitText: $text, $newCursorPosition")
                nodeViews?.getOrNull(currEditIndex)?.commitText(text.toString())
                invalidate()
                return true
            }

            override fun commitCompletion(text: CompletionInfo?): Boolean {
                log.logStr("commitCompletion: $text")
                if (text == null) return false
                nodeViews?.getOrNull(currEditIndex)?.commitText(text.text.toString())
                return super.commitCompletion(text)
            }

            override fun commitContent(inputContentInfo: InputContentInfo, flags: Int, opts: Bundle?): Boolean {
                log.logStr("commitContent: $inputContentInfo, $flags, $opts")
                return super.commitContent(inputContentInfo, flags, opts)
            }

            override fun commitText(text: CharSequence, newCursorPosition: Int, textAttribute: TextAttribute?): Boolean {
                log.logStr("commitText: $text, $newCursorPosition, $textAttribute")
                return super.commitText(text, newCursorPosition, textAttribute)
            }

            override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
                log.logStr("deleteSurroundingText: $beforeLength, $afterLength")
                val edit = editable ?: return true
                edit.delete(0, edit.length)
                return true
            }

            override fun performContextMenuAction(id: Int): Boolean {
                log.logStr("performContextMenuAction: $id")
                if (id == android.R.id.selectAll || id == android.R.id.copy) {
                    return true
                }
                return super.performContextMenuAction(id)
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        log.logStr("onKeyDown: $keyCode, ${event?.action}, ${event?.isPrintingKey}")
        when (keyCode) {
            KeyEvent.KEYCODE_DEL -> {
                nodeViews?.getOrNull(currEditIndex)?.deleteText()
                invalidate()
                return true
            }

            KeyEvent.KEYCODE_ENTER -> {
                nodeViews?.getOrNull(currEditIndex)?.commitText("\n")
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
                nodeViews?.getOrNull(currEditIndex)?.noSelect() // 收回键盘时取消选中状态
                invalidate()
                noCurrEditIndex()
            }
            log.logStr("onBoardChange: $it")
        }
    }

    private fun noCurrEditIndex() {
        currEditIndex = -1
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
        currCursorJob.stopEditMode()
        if (SoftKeyBoardHelper.im.isActive) {
            SoftKeyBoardHelper.hide(this)
        }
        nodeViews?.getOrNull(currEditIndex)?.noSelect()
        noCurrEditIndex()
    }


    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        currMode = Mode.Center
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
    fun withBitmap(any: (Bitmap) -> Unit) {
        nodeViews?.find { it.isSelected }?.isSelected = false // 重置选择状态
        matrix { centerContent() } // 分享时先居中，否则只能分享成缩放后的样子
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
    fun withBitmap2File(file: File, any: (File) -> Unit) {
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
     * 编辑模式时，增加的宽度
     */
    fun editSpace(node: NodeView): Float {
        return 10f
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
        val rect = RectF().apply { matrix.mapRect(this, content) }
        rect.right += node.wPadding * 2

        val containerRect = RectF(0f, 0f, width.toFloat(), height.toFloat())
        val offX = containerRect.centerX() - rect.centerX()
        val offY = containerRect.centerY() - rect.centerY()

        // 不使用调整过的值
        rect.set(content)
        rect.right += node.wPadding * 2
        node.editRect = rect

        matrix.postTranslate(offX, offY)/*val scale = containerRect.width() / rect.width()
        log.logStr("adjustCenter4SelectRect: $scale")
        matrix.postScale(scale, scale, offX, offY)*/
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo?): InputConnection {
        outAttrs?.imeOptions = EditorInfo.IME_ACTION_DONE
        outAttrs?.inputType = InputType.TYPE_CLASS_TEXT
        return inputConn
    }

    private fun showInput() {
        SoftKeyBoardHelper.show(this)
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
            views.forEach {
                if (it.isSelected) {
                    currStyle.drawEditNode(this@MindMapView, this, it)
                } else {
                    currStyle.drawNode(this, it)
                }
            }
        }
    }

    open class NodeView(
        val name: String, // 标题
        val level: Int, // 层级，大多数样式都跟层级有关
        val spaceRect: RectF, // 节点占用位置
        val contentRealRect: RectF, // 节点显示区域
        var linkPoint: LinkPoint? = null, // 子节点到其父节点的连接点
        // 选中相关
        var isSelected: Boolean = false, // 预留菜单选择
        var isEditSelected: Boolean = false, // 节点编辑模式
        var editRect: RectF? = null, // 因为编辑框要比内容框更大(初次显示时内容+光标)，所以单独保存
        var editName: String? = null, // 编辑后保存的文字
    ) {
        var showCursor: Boolean = false // 显示光标，需要isEditSelected=true，值会定时切换

        val contentRect get() = editRect ?: contentRealRect

        /**
         * 编辑节点
         */
        fun commitText(toString: String) {
            if (!isEditSelected) return
            editName += toString
        }

        fun noSelect() {
            isEditSelected = false
            isSelected = false
            editRect = null
            editName = null
        }

        fun startEditMode() {
            editName = name
            showCursor = true
        }

        fun deleteText() {
            if (!isEditSelected) return
            val name = editName ?: return
            if (name.isNotEmpty()) {
                editName = name.substring(0, name.length - 1)
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

        /**
         * 编辑节点绘制
         * 替代drawNode
         */
        fun drawEditNode(view: MindMapView, canvas: Canvas, node: NodeView) {
            MindMapCommon.drawEditNode(view, canvas, node)
        }
    }

    private sealed class Mode {
        data object Drag : Mode()
        data object Scale : Mode()
        data object Center : Mode()

        val isCenter get() = this is Center
    }

    private class CursorJob(private val view: View) {
        private var currJob: Job? = null

        @WorkerThread
        suspend fun startEditMode(node: NodeView?) {
            stopEditMode()
            node ?: return
            val newJob = Job()
            currJob = newJob
            node.startEditMode() // todo stopEditMode调用时在等待中导致并没有断掉当前循环就开始下一个循环
            while (currJob?.isActive == true) {
                delay(500L)
                node.showCursor = !node.showCursor
                withContext(Dispatchers.Main) { view.invalidate() }
            }
        }

        fun stopEditMode() {
            currJob?.cancel()
            currJob = null
        }

    }
}