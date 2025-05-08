package com.munch1182.lib.base

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * 使用PowerManager的方式已经不被建议
 */
fun Activity.keepScreenOn() {
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
}

fun Activity.clearScreenOn() {
    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
}

/**
 * 获取可显示内容屏幕的宽高
 */
fun screenDisplay(): DisplayMetrics = ctx.resources.displayMetrics

/**
 * 获取屏幕宽高
 */
fun screen(): Rect {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        wm.currentWindowMetrics.bounds
    } else {
        DisplayMetrics().apply {
            @Suppress("DEPRECATION")
            (wm.defaultDisplay).getRealMetrics(this)
        }.let {
            Rect(0, 0, it.widthPixels, it.heightPixels)
        }
    }
}

fun Activity.statusHeight(): Int {
    return ViewCompat.getRootWindowInsets(window.decorView)?.let {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            it.getInsets(WindowInsetsCompat.Type.statusBars()).top
        } else {
            @Suppress("DEPRECATION")
            it.systemWindowInsetTop
        }
    } ?: 0
}

@SuppressLint("InternalInsetResource", "DiscouragedApi")
fun statusHeight(context: Context = ctx): Int {
    val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
    if (resourceId > 0) {
        return context.resources.getDimensionPixelSize(resourceId)
    }
    return 0
}


/**
 * 当使用全面屏手势更改底部导航栏显示后，获取的值会随之更改
 */
@SuppressLint("InternalInsetResource", "DiscouragedApi")
fun navigationHeight(context: Context = ctx): Int {
    val resourceId = context.resources.getIdentifier("navigation_bar_height", "dimen", "android")
    if (resourceId > 0) {
        return context.resources.getDimensionPixelSize(resourceId)
    }
    return 0
}
