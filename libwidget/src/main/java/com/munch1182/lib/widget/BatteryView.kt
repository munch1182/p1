package com.munch1182.lib.widget

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView

class BatteryView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr) {

    private val batteryImage by lazy { ImageView(context) }
    private val bg by lazy { ImageView(context) }
    private val batteryText by lazy { TextView(context) }
    private var _level = 100

    val level get() = _level

    init {
        val lpW = LayoutParams.WRAP_CONTENT
        bg.setImageResource(R.drawable.baseline_battery_0_bar_24)
        batteryImage.setImageResource(R.drawable.bird_stat_sys_battery_level_list)
        addView(bg)
        addView(batteryImage, LayoutParams(lpW, lpW).apply { gravity = Gravity.CENTER })
        addView(batteryText, LayoutParams(lpW, lpW).apply { gravity = Gravity.CENTER })
        batteryImage.setImageLevel(_level)
        setDark(true)
        batteryText.text = _level.toString()
        batteryText.apply {
            textSize = 6f
            includeFontPadding = false
            setTextColor(0xFF000000.toInt())
        }
    }

    fun setLevel(level: Int) {
        _level = level / 10 * 10
        if (level < 0) _level = 0
        if (level > 100) _level = 100
        batteryImage.setImageLevel(level)
        batteryText.text = level.toString()
    }

    fun setDark(isDark: Boolean = true) {
        if (isDark) {
            bg.imageTintList = ColorStateList(arrayOf(intArrayOf()), intArrayOf(0xFF000000.toInt()))
        } else {
            bg.imageTintList = null
        }
    }
}