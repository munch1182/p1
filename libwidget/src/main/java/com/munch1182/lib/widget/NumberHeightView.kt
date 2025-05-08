package com.munch1182.lib.widget

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.graphics.toColorInt
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.max

class NumberHeightView @JvmOverloads constructor(
    ctx: Context, set: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0
) : View(ctx, set, defStyleAttr, defStyleRes) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val param = Param()

    private var useWidth = 0
    private var itemSpace = 0f
    private var maxHeight = 0f
    private var itemRadius = 0f
    private var rulerRadius = 0f
    private var valueHeight = 0f

    private val rulerRect = RectF()
    private val itemRect = RectF()
    private val lock = ReentrantLock()

    private val itemHeight = mutableListOf<Float>()
    private val itemHeightTmp = mutableListOf<Float>()
    private var anim = anim()
    private var currTran = 0f
    private var animDuration = 100L

    init {
        ctx.obtainStyledAttributes(set, R.styleable.NumberHeightView, defStyleAttr, defStyleRes).use { param.parse(it) }
    }

    fun start() {
        stop()
        anim = anim()
        anim?.start()
    }

    fun stop() {
        anim?.pause()
        anim = null
    }

    fun setDuration(duration: Long): NumberHeightView {
        this.animDuration = duration
        return this
    }

    private fun anim(): ValueAnimator? {
        return ValueAnimator.ofFloat(0f, itemSpace * 2 + param.itemWidth).apply {
            duration = this@NumberHeightView.animDuration
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = LinearInterpolator()
            addUpdateListener {
                currTran = -(it.animatedValue as Float)
                postInvalidate()
            }
        }
    }

    fun addNumber(number: Int) {
        lock.withLock {
            val itemHeight: Float = if (number <= param.minValue) {
                param.minHeight.toFloat()
            } else if (number >= param.maxValue) {
                param.maxHeight.toFloat()
            } else {
                (number - param.minValue) * valueHeight
            }
            this.itemHeight.add(param.itemNumber / 2 + 1, itemHeight)
            if (this.itemHeight.size > param.itemNumber) {
                this.itemHeight.removeAt(0)
            }
            //post { invalidate() }
        }
    }

    private fun calculateValues() {
        lock.withLock {
            val nums = param.itemNumber
            itemSpace = param.itemSpace.toFloat()
            useWidth = measuredWidth - paddingStart - paddingEnd
            if (itemSpace == -1f) {
                itemSpace = max((useWidth - nums * param.itemWidth) / (nums + 1f), 10f)
            }
            maxHeight = param.maxHeight.toFloat()
            if (maxHeight == -1f) {
                maxHeight = (measuredHeight - paddingTop - paddingBottom).toFloat()
            }
            itemRadius = param.itemWidth / 2f
            rulerRadius = param.rulerWidth / 2f

            val wCenter = paddingStart + useWidth / 2f
            rulerRect.set(wCenter - param.rulerWidth / 2f, paddingTop.toFloat(), wCenter + param.rulerWidth / 2f, (measuredHeight - paddingBottom).toFloat())

            valueHeight = (maxHeight - param.minHeight) / (param.maxValue - param.minValue)

            itemHeight.clear()
            for (i in 0 until nums) {
                itemHeight.add(if (i >= nums / 2) param.minHeight.toFloat() else 0f)
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val centerHeight = height / 2
        paint.setColor(param.colorNoValue)
        lock.withLock {
            itemHeightTmp.clear()
            itemHeightTmp.addAll(itemHeight)
        }
        var start = paddingStart + itemSpace + currTran
        lock.withLock { itemHeightTmp }.forEachIndexed { i, fl ->
            if (i > itemHeight.size / 2) {
                paint.setColor(param.colorNoValue)
            } else {
                paint.setColor(param.colorValue)
            }
            val halfHeight = fl / 2F
            val left = start
            val top = centerHeight - halfHeight
            val right = left + param.itemWidth
            val bottom = centerHeight + halfHeight
            itemRect.set(left, top, right, bottom)
            canvas.drawRoundRect(itemRect, itemRadius, itemRadius, paint)
            start = right + itemSpace
        }
        if (param.showRuler) {
            paint.setColor(param.rulerColor)
            canvas.drawRoundRect(rulerRect, rulerRadius, rulerRadius, paint)
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        calculateValues()
    }

    class Param(
        var colorValue: Int = Color.BLACK,
        var colorNoValue: Int = "#c3c3c3".toColorInt(),
        var itemNumber: Int = 40,
        var itemWidth: Int = 10,
        var itemSpace: Int = -1,
        var minHeight: Int = itemWidth * 2,
        var maxHeight: Int = -1,
        var minValue: Float = 0f,
        var maxValue: Float = 100f,
        var showRuler: Boolean = true,
        var rulerWidth: Int = itemWidth,
        var rulerColor: Int = "#33c3c3c3".toColorInt()
    ) {
        fun parse(it: TypedArray) {
            colorValue = it.getColor(R.styleable.NumberHeightView_colorValued, colorValue)
            colorNoValue = it.getColor(R.styleable.NumberHeightView_colorNoValue, colorNoValue)
            itemNumber = it.getInt(R.styleable.NumberHeightView_itemNumber, itemNumber)
            itemWidth = it.getDimensionPixelSize(R.styleable.NumberHeightView_itemWidth, itemWidth)
            itemSpace = it.getDimensionPixelSize(R.styleable.NumberHeightView_itemSpace, itemSpace)
            minHeight = it.getDimensionPixelSize(R.styleable.NumberHeightView_minHeight, minHeight)
            maxHeight = it.getDimensionPixelSize(R.styleable.NumberHeightView_maxHeight, maxHeight)
            minValue = it.getFloat(R.styleable.NumberHeightView_minValue, minValue)
            maxValue = it.getFloat(R.styleable.NumberHeightView_maxValue, maxValue)
            showRuler = it.getBoolean(R.styleable.NumberHeightView_showRuler, showRuler)
            rulerWidth = it.getDimensionPixelSize(R.styleable.NumberHeightView_rulerWidth, rulerWidth)
            rulerColor = it.getColor(R.styleable.NumberHeightView_rulerColor, rulerColor)
        }
    }
}