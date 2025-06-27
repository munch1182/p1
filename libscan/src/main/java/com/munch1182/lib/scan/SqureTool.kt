package com.munch1182.lib.scan

import org.opencv.core.Mat
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc

object SqureTool {

    private val scaler = Scalar(255.0, 136.0, 136.0, 136.0)
    private val rect = Rect()

    fun centerRectDraw2(mat: Mat, min: Int) {
        val w = mat.width()
        val h = mat.height()
        rect.x = w / 2 - min / 2
        rect.y = h / 2 - min / 2
        rect.width = min
        rect.height = min
        return Imgproc.rectangle(mat, rect, scaler)
    }

    fun centerRectDraw2Cop(mat: Mat, min: Int): Mat {
        val w = mat.width()
        val h = mat.height()
        rect.x = w / 2 - min / 2
        rect.y = h / 2 - min / 2
        rect.width = min
        rect.height = min
        return Mat(mat, rect)
    }

}