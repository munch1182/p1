package com.munch1182.lib.widget.recyclerview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.munch1182.lib.base.drawTextInCenter
import com.munch1182.lib.base.height
import com.munch1182.lib.base.sp2Px


class SideIndexBarView @JvmOverloads constructor(context: Context, set: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0) : View(context, set, defStyleAttr, defStyleRes) {


    private var update: Paint.() -> Unit = {
        color = Color.BLACK
        textSize = 16.sp2Px
    }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val texts = mutableListOf("1", "2", "3", "a", "b", "c", "e")
    private var updateSelectCanvas: (Canvas.(Int, Float, Float, Paint) -> Unit)? = null
    private var updateSelectPaint: (Paint.() -> Unit)? = null
    private var selectIndex = 0

    // 监听rv的滑动事件，同步选择
    fun attachRecyclerView(rv: RecyclerView) {}

    fun updatePaint(update: Paint.() -> Unit): SideIndexBarView {
        this.update = update
        return this
    }

    fun updatePaintSelect(select: Paint.() -> Unit): SideIndexBarView {
        this.updateSelectPaint = select
        return this
    }

    fun updateCanvasSelect(update: Canvas.(Int, Float, Float, Paint) -> Unit): SideIndexBarView {
        this.updateSelectCanvas = update
        return this
    }

    fun setIndexData(list: Array<String>) {
        this.texts.clear()
        this.texts.addAll(list)
        invalidate()
    }

    fun select(index: Int) {
        this.selectIndex = index
    }

    override fun onDraw(c: Canvas) {
        val count = texts.size
        val startCX = width / 2f
        val h = (height - paddingTop - paddingBottom) / count
        var startY = paddingTop.toFloat() + h / 2f

        this.update(paint)

        texts.forEachIndexed { i, s ->
            if (i == selectIndex) {
                this.updateSelectPaint?.invoke(paint)
                if (this.updateSelectCanvas != null) {
                    this.updateSelectCanvas?.invoke(c, i, startCX, startY, paint)
                } else {
                    c.drawTextInCenter(s, startCX, startY, paint)
                }
            } else {
                c.drawTextInCenter(s, startCX, startY, paint)
            }
            startY += h
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val wMode = MeasureSpec.getMode(widthMeasureSpec)
        var width = 0
        val measureWH = measureAllText(texts.toTypedArray())
        when (wMode) {
            MeasureSpec.EXACTLY -> width = MeasureSpec.getSize(widthMeasureSpec)
            MeasureSpec.AT_MOST,
            MeasureSpec.UNSPECIFIED -> width = measureWH.first.toInt()
        }

        val hMode = MeasureSpec.getMode(heightMeasureSpec)
        var height = 0
        when (hMode) {
            MeasureSpec.EXACTLY -> height = MeasureSpec.getSize(heightMeasureSpec)
            MeasureSpec.AT_MOST,
            MeasureSpec.UNSPECIFIED -> height = measureWH.second.toInt()
        }

        setMeasuredDimension(width + paddingStart + paddingEnd, height + paddingTop + paddingBottom)
    }

    private fun measureAllText(texts: Array<String>): Pair<Float, Float> {
        this.update(paint)
        val h = paint.height
        var maxW = 0f
        val maxH = h * texts.size
        texts.forEach {
            val width = paint.measureText(it)
            maxW = maxOf(maxW, width)
        }
        return maxW to maxH
    }
}