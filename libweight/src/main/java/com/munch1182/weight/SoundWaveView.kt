package com.munch1182.weight

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.toColorInt
import kotlin.math.min

class SoundWaveView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // 配置参数
    private var waveColor: Int = "#F46765".toColorInt()
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

    // 数据存储 - 使用固定大小的环形缓冲区
    private val waveBuffer = WaveBuffer()
    private var bufferSize = 100  // 默认缓冲区大小，会在onSizeChanged中重新计算

    // 动画相关
    private var animator: ValueAnimator? = null
    private var currentTime = 0L
    private var animationSpeed = 50L // 毫秒/每像素
    private var isAnimating = false

    // 性能优化
    private var lastAmplitudeCount = 0
    private var viewWidth = 0
    private var viewHeight = 0

    // 添加频率控制
    private var lastAddTime = 0L
    private var minAddInterval = 50L // 最小添加间隔，毫秒

    // 最大波形数量限制
    private var maxVisibleWaves = 0

    private val centerPoint = PointF()
    private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 2f
    }

    init {
        setupPaint()
    }

    private fun setupPaint() {
        paint.color = waveColor
        gradientPaint.color = waveColor
        centerPaint.color = waveColor
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewWidth = w
        viewHeight = h

        // 根据视图宽度计算最大可见波形数量（加上一些额外缓冲）
        maxVisibleWaves = calculateMaxVisibleWaves()
        bufferSize = maxVisibleWaves * 2 // 缓冲区大小为可见区域的两倍

        // 重新初始化缓冲区
        waveBuffer.resize(bufferSize)

        // 重新计算已有波形的高度
        waveBuffer.recalculateHeights(viewHeight, maxAmplitude)
        centerPoint.set(viewWidth / 2f, viewHeight / 2f)
    }

    /**
     * 计算最大可见波形数量
     */
    private fun calculateMaxVisibleWaves(): Int {
        if (waveWidth + waveSpacing <= 0) return 100
        // 计算屏幕内最多能显示多少个波形
        val visibleWidth = viewWidth + waveWidth * 2 // 加上左右缓冲
        return (visibleWidth / (waveWidth + waveSpacing)).toInt() + 1
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
     * 设置最小添加间隔（防止添加过快）
     */
    fun setMinAddInterval(interval: Long) {
        minAddInterval = interval
    }

    /**
     * 设置最大振幅（用于归一化）
     */
    fun setMaxAmplitude(maxAmplitude: Float) {
        this.maxAmplitude = maxAmplitude.coerceAtLeast(0.01f)
        waveBuffer.recalculateHeights(viewHeight, maxAmplitude)
        invalidate()
    }

    /**
     * 设置波形宽度
     */
    fun setWaveWidth(width: Float) {
        waveWidth = width.dpToPx()
        maxVisibleWaves = calculateMaxVisibleWaves()
        invalidate()
    }

    /**
     * 设置波形间距
     */
    fun setWaveSpacing(spacing: Float) {
        waveSpacing = spacing.dpToPx()
        maxVisibleWaves = calculateMaxVisibleWaves()
        invalidate()
    }

    /**
     * 添加单个振幅数据到右侧末尾
     * @param amplitude 振幅值（0-1之间）
     */
    fun addAmplitude(amplitude: Float) {
        val currentTime = System.currentTimeMillis()

        // 频率限制：避免添加过快
        if (currentTime - lastAddTime < minAddInterval) {
            return
        }
        lastAddTime = currentTime

        // 归一化振幅
        val normalizedAmplitude = amplitude.coerceIn(0.02f, 1f)

        // 如果缓冲区已满，移除最旧的数据
        if (waveBuffer.isFull()) {
            waveBuffer.removeOldest()
        }

        // 添加新数据到右侧
        waveBuffer.add(
            WaveEntry(
                amplitude = normalizedAmplitude,
                x = viewWidth.toFloat(), // 从最右侧开始
                height = calculateWaveHeight(normalizedAmplitude)
            )
        )

        // 如果条目数量超过最大可见数量，移除超出部分
        while (waveBuffer.size() > maxVisibleWaves * 1.5) {
            waveBuffer.removeOldest()
        }

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
        waveBuffer.clear()

        // 限制振幅数量，只取最新的数据
        val limitedAmplitudes = if (amplitudes.size > maxVisibleWaves) {
            amplitudes.copyOfRange(amplitudes.size - maxVisibleWaves, amplitudes.size)
        } else {
            amplitudes
        }

        // 从右侧开始添加所有振幅
        var x = viewWidth.toFloat()
        limitedAmplitudes.reversedArray().forEach { amplitude ->
            val normalizedAmplitude = amplitude.coerceIn(0f, 1f)
            if (!waveBuffer.isFull()) {
                waveBuffer.add(
                    WaveEntry(
                        amplitude = normalizedAmplitude,
                        x = x,
                        height = calculateWaveHeight(normalizedAmplitude)
                    )
                )
                x -= (waveWidth + waveSpacing)
            }
        }

        // 反转列表，使最新的在最右侧
        waveBuffer.reverse()

        startAnimation()
        invalidate()
    }

    /**
     * 清空所有波形
     */
    fun clear() {
        stopAnimation()
        waveBuffer.clear()
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
     * 开始动画
     */
    fun startAnimation() {
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
        if (waveBuffer.isEmpty() || animationSpeed == 0L) return

        val currentTime = System.currentTimeMillis()
        val deltaTime = (currentTime - this.currentTime).coerceAtMost(100L) // 限制最大100ms
        this.currentTime = currentTime

        // 计算移动距离（基于时间和速度）
        val moveDistance = (deltaTime / animationSpeed.toFloat()) * (waveWidth + waveSpacing)

        // 更新所有条目的位置，向左移动
        waveBuffer.updatePositions(-moveDistance)

        // 移除完全离开屏幕左侧的波形
        waveBuffer.removeIf { entry ->
            entry.x + waveWidth < 0
        }
    }

    /**
     * 重启动画（用于速度变化时）
     */
    private fun restartAnimation() {
        stopAnimation()
        if (!waveBuffer.isEmpty()) {
            startAnimation()
        }
    }

    /**
     * 停止动画
     */
    fun stopAnimation() {
        isAnimating = false
        animator?.cancel()
        animator = null
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (waveBuffer.isEmpty()) return

        val centerY = centerPoint.y

        // 绘制所有波形条目
        waveBuffer.forEach { entry ->
            if (entry.x + waveWidth >= 0 && entry.x <= viewWidth) { // 只在可见区域绘制
                drawWave(canvas, entry, centerY)
            }
        }
        canvas.drawLine(0f, centerY, viewWidth.toFloat(), centerY, centerPaint)

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
        //drawHighlight(canvas, left, top, right, bottom, cornerRadius)
    }

    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        alpha = 60
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
     */
    private data class WaveEntry(
        val amplitude: Float,
        var x: Float,
        val height: Float  // 高度在创建时就确定，不会变化
    )

    /**
     * 波形缓冲区 - 环形缓冲区实现，避免频繁创建/销毁对象
     */
    private inner class WaveBuffer {
        private var buffer: Array<WaveEntry?> = arrayOfNulls(bufferSize)
        private var start = 0
        private var end = 0
        private var count = 0

        fun resize(newSize: Int) {
            val newBuffer = arrayOfNulls<WaveEntry>(newSize)
            val copyCount = min(count, newSize)

            for (i in 0 until copyCount) {
                newBuffer[i] = buffer[(start + i) % buffer.size]
            }

            buffer = newBuffer
            start = 0
            end = copyCount % newSize
            count = copyCount
        }

        fun add(entry: WaveEntry) {
            buffer[end] = entry
            end = (end + 1) % buffer.size
            if (count == buffer.size) {
                start = (start + 1) % buffer.size
            } else {
                count++
            }
        }

        fun removeOldest() {
            if (count > 0) {
                buffer[start] = null
                start = (start + 1) % buffer.size
                count--
            }
        }

        fun removeIf(predicate: (WaveEntry) -> Boolean) {
            var i = 0
            while (i < count) {
                val index = (start + i) % buffer.size
                val entry = buffer[index] ?: break

                if (predicate(entry)) {
                    // 移除该元素
                    for (j in i until count - 1) {
                        val currentIndex = (start + j) % buffer.size
                        val nextIndex = (start + j + 1) % buffer.size
                        buffer[currentIndex] = buffer[nextIndex]
                    }
                    buffer[(start + count - 1) % buffer.size] = null
                    end = (end - 1 + buffer.size) % buffer.size
                    count--
                } else {
                    i++
                }
            }
        }

        fun updatePositions(deltaX: Float) {
            for (i in 0 until count) {
                val index = (start + i) % buffer.size
                buffer[index]?.x = buffer[index]?.x?.plus(deltaX) ?: 0f
            }
        }

        fun recalculateHeights(viewHeight: Int, maxAmplitude: Float) {
            for (i in 0 until count) {
                val index = (start + i) % buffer.size
                val entry = buffer[index] ?: continue
                val normalized = entry.amplitude / maxAmplitude
                val height = normalized.coerceIn(0f, 1f) * viewHeight * 0.8f
                // 注意：由于WaveEntry是不可变数据类，我们需要替换整个对象
                buffer[index] = entry.copy(height = height)
            }
        }

        fun clear() {
            buffer.fill(null)
            start = 0
            end = 0
            count = 0
        }

        fun isEmpty() = count == 0
        fun isFull() = count == buffer.size
        fun size() = count

        fun forEach(action: (WaveEntry) -> Unit) {
            for (i in 0 until count) {
                val index = (start + i) % buffer.size
                val entry = buffer[index] ?: continue
                action(entry)
            }
        }

        fun reverse() {
            if (count <= 1) return

            val tempList = mutableListOf<WaveEntry>()
            forEach { tempList.add(it) }
            tempList.reverse()

            clear()
            tempList.forEach { add(it) }
        }
    }

    // 扩展函数：dp转px
    private fun Float.dpToPx(): Float {
        return this * context.resources.displayMetrics.density
    }
}