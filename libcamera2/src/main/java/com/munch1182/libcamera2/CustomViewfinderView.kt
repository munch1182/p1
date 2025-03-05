package com.munch1182.libcamera2

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import com.journeyapps.barcodescanner.ViewfinderView

class CustomViewfinderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ViewfinderView(context, attrs) {

    override fun onDraw(canvas: Canvas) {
        // todo 绘制遮罩
    }
}