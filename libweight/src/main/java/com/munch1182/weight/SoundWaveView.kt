package com.munch1182.weight

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class SoundWaveView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // 配置参数
    private var waveColor: Int = Color.RED
    private var waveWidth: Float = 2f.dpToPx()
    private var waveSpacing: Float = 2f.dpToPx()
    private var maxAmplitude: Float = 1.0f

    // 绘画工具
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val gradientPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    // 数据存储 - 存储振幅值和是否已初始化高度的标志
    private val waveEntries = mutableListOf<WaveEntry>()

    // 动画相关
    private var animator: ValueAnimator? = null
    private var currentTime = 0L
    private var animationSpeed = 50L // 毫秒/每像素
    private var isAnimating = false

    // 性能优化
    private var lastAmplitudeCount = 0
    private var viewWidth = 0
    private var viewHeight = 0

    init {
        setupPaint()
    }

    private fun setupPaint() {
        paint.color = waveColor
        gradientPaint.color = waveColor
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewWidth = w
        viewHeight = h
    }

    /**
     * 设置动画速度
     * @param speed 每像素移动所需时间（毫秒），值越小移动越快
     */
    fun setAnimationSpeed(speed: Long) {
        animationSpeed = speed
        restartAnimation()
    }

    /**
     * 设置最大振幅（用于归一化）
     */
    fun setMaxAmplitude(maxAmplitude: Float) {
        this.maxAmplitude = maxAmplitude.coerceAtLeast(0.01f)
        invalidate()
    }

    /**
     * 添加单个振幅数据到右侧末尾
     * @param amplitude 振幅值（0-1之间）
     */
    fun addAmplitude(amplitude: Float) {
        // 创建新的波形条目，添加到右侧末尾（x坐标为viewWidth）
        val normalizedAmplitude = amplitude.coerceIn(0.1f, 1f)

        waveEntries.add(
            WaveEntry(
                amplitude = normalizedAmplitude,
                x = viewWidth.toFloat(), // 从最右侧开始
                height = calculateWaveHeight(normalizedAmplitude) // 立即计算高度
            )
        )

        // 如果条目数量超过可视区域，移除最左边的
        removeOffscreenEntries()

        // 启动或继续动画
        if (!isAnimating) {
            startAnimation()
        }

        invalidate()
    }

    /**
     * 批量设置振幅数据
     * @param amplitudes 振幅数组（0-1之间）
     */
    fun setAmplitudes(amplitudes: FloatArray) {
        waveEntries.clear()

        // 从右侧开始添加所有振幅
        var x = viewWidth.toFloat()
        amplitudes.reversedArray().forEach { amplitude ->
            val normalizedAmplitude = amplitude.coerceIn(0f, 1f)
            waveEntries.add(
                WaveEntry(
                    amplitude = normalizedAmplitude,
                    x = x,
                    height = calculateWaveHeight(normalizedAmplitude)
                )
            )
            x -= (waveWidth + waveSpacing)
        }

        // 反转列表，使最新的在最右侧
        waveEntries.reverse()

        startAnimation()
        invalidate()
    }

    /**
     * 清空所有波形
     */
    fun clear() {
        stopAnimation()
        waveEntries.clear()
        invalidate()
    }

    /**
     * 计算波形高度（基于振幅和视图高度）
     */
    private fun calculateWaveHeight(amplitude: Float): Float {
        // 使用最大振幅进行归一化
        val normalized = amplitude / maxAmplitude
        // 返回高度（使用视图高度的80%）
        return normalized.coerceIn(0f, 1f) * viewHeight * 0.8f
    }

    /**
     * 移除屏幕外的条目
     */
    private fun removeOffscreenEntries() {
        // 移除所有 x 坐标小于 -waveWidth 的条目（完全移出左侧屏幕）
        waveEntries.removeAll { it.x + waveWidth < 0 }
    }

    /**
     * 开始动画
     */
    private fun startAnimation() {
        if (isAnimating) return

        isAnimating = true
        currentTime = System.currentTimeMillis()

        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = Long.MAX_VALUE // 无限动画
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { animation ->
                updateWavePositions()
                invalidate()
            }
            start()
        }
    }

    /**
     * 更新波形位置
     */
    private fun updateWavePositions() {
        if (waveEntries.isEmpty() || animationSpeed == 0L) return

        val currentTime = System.currentTimeMillis()
        val deltaTime = (currentTime - this.currentTime).coerceAtMost(100L) // 限制最大100ms
        this.currentTime = currentTime

        // 计算移动距离（基于时间和速度）
        val moveDistance = (deltaTime / animationSpeed.toFloat()) * (waveWidth + waveSpacing)

        // 更新所有条目的位置，向左移动
        for (entry in waveEntries) {
            entry.x -= moveDistance
        }

        // 移除屏幕外的条目
        removeOffscreenEntries()
    }

    /**
     * 重启动画（用于速度变化时）
     */
    private fun restartAnimation() {
        stopAnimation()
        if (waveEntries.isNotEmpty()) {
            startAnimation()
        }
    }

    /**
     * 停止动画
     */
    private fun stopAnimation() {
        isAnimating = false
        animator?.cancel()
        animator = null
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (waveEntries.isEmpty()) return

        val centerY = viewHeight / 2f

        // 绘制所有波形条目
        waveEntries.forEach { entry ->
            if (entry.x + waveWidth >= 0 && entry.x <= viewWidth) { // 只在可见区域绘制
                drawWave(canvas, entry, centerY)
            }
        }
    }

    /**
     * 绘制单个波形
     */
    private fun drawWave(canvas: Canvas, entry: WaveEntry, centerY: Float) {
        val waveHeight = entry.height

        // 计算竖线的边界
        val left = entry.x
        val top = centerY - waveHeight / 2
        val right = left + waveWidth
        val bottom = centerY + waveHeight / 2

        // 创建渐变效果
        /*val gradient = LinearGradient(
            left, top,
            left, bottom,
            intArrayOf(
                Color.argb(100, Color.red(waveColor), Color.green(waveColor), Color.blue(waveColor)),
                waveColor,
                Color.argb(100, Color.red(waveColor), Color.green(waveColor), Color.blue(waveColor))
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )

        gradientPaint.shader = gradient*/

        // 绘制圆角矩形（竖线）
        val cornerRadius = waveWidth / 2
        canvas.drawRoundRect(
            left, top, right, bottom,
            cornerRadius, cornerRadius,
            gradientPaint
        )

        // 绘制高光效果
        drawHighlight(canvas, left, top, right, bottom, cornerRadius)
    }

    /**
     * 绘制高光效果
     */
    private fun drawHighlight(
        canvas: Canvas,
        left: Float, top: Float,
        right: Float, bottom: Float,
        cornerRadius: Float
    ) {
        val highlightWidth = waveWidth / 3
        val highlightLeft = left + waveWidth / 3

        val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = 60
        }

        canvas.drawRoundRect(
            highlightLeft, top + cornerRadius,
            highlightLeft + highlightWidth, bottom - cornerRadius,
            highlightWidth / 2, highlightWidth / 2,
            highlightPaint
        )
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimation()
    }

    /**
     * 波形条目数据类
     * @param amplitude 振幅值
     * @param x 当前x坐标
     * @param height 计算好的高度（立即计算，不会变化）
     */
    private data class WaveEntry(
        val amplitude: Float,
        var x: Float,
        val height: Float  // 高度在创建时就确定，不会变化
    )

    // 扩展函数：dp转px
    private fun Float.dpToPx(): Float {
        return this * context.resources.displayMetrics.density
    }
}