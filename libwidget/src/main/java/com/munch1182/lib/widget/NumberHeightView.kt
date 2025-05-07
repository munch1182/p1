package com.munch1182.lib.widget

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.toColorInt

class NumberHeightView @JvmOverloads constructor(
    ctx: Context, set: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0
) : View(ctx, set, defStyleAttr, defStyleRes) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val param = Param()

    init {
        ctx.obtainStyledAttributes(set, R.styleable.NumberHeightView, defStyleAttr, defStyleRes)
            .use { param.parse(it) }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    class Param(
        var colorValue: Int = Color.BLACK,
        var colorNoValue: Int = "#c3c3c3".toColorInt(),
        var secNum: Int = 10,
        var itemWidth: Int = 10,
        var minHeight: Int = -1,
        var maxHeight: Int = -1,
        var minValue: Float = 0f,
        var maxValue: Float = 100f,
        var rulerWidth: Int = 10,
        var rulerColor: Int = "#c3c3c3".toColorInt()
    ) {
        fun parse(it: TypedArray) {
            colorValue = it.getColor(R.styleable.NumberHeightView_colorValued, colorValue)
            colorNoValue = it.getColor(R.styleable.NumberHeightView_colorNoValue, colorNoValue)
            secNum = it.getInt(R.styleable.NumberHeightView_secNum, secNum)
            itemWidth = it.getDimensionPixelSize(R.styleable.NumberHeightView_itemWidth, itemWidth)
            minHeight = it.getDimensionPixelSize(R.styleable.NumberHeightView_minHeight, minHeight)
            maxHeight = it.getDimensionPixelSize(R.styleable.NumberHeightView_maxHeight, maxHeight)
            minValue = it.getFloat(R.styleable.NumberHeightView_minValue, minValue)
            maxValue = it.getFloat(R.styleable.NumberHeightView_maxValue, maxValue)
            rulerWidth =
                it.getDimensionPixelSize(R.styleable.NumberHeightView_rulerWidth, rulerWidth)
            rulerColor = it.getColor(R.styleable.NumberHeightView_rulerColor, rulerColor)
        }
    }
}