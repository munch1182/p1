package com.munch1182.lib

import android.app.Activity
import android.view.WindowManager

/**
 * 使用PowerManager的方式已经不被建议
 */
fun Activity.keepScreenOn(){
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
}

fun Activity.clearScreenOn(){
    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
}