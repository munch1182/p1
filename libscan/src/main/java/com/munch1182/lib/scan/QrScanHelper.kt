package com.munch1182.lib.scan

import android.content.Context
import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

class QrScanHelper(private var listener: OnQrCodeListener? = null) {

    private var dstRgb: Mat? = null
    private var m: Mat? = null
    private val center = Point()
    private var size: Size? = null
    private val wxQr by lazy { WechatQr.new() }


    fun detectAndDecode(ctx: Context, resourceId: Int): List<String> {
        val mat = Utils.loadResource(ctx, resourceId)
        return wxQr.detectAndDecode(mat)
    }

    fun detectAndDecode(bitmap: Bitmap): List<String> {
        val newMat = Mat()
        Utils.bitmapToMat(bitmap, newMat)
        return wxQr.detectAndDecode(newMat)
    }

    fun onCameraFrame(frame: CameraBridgeViewBaseNew.CvCameraViewFrame?): Mat {
        dstRgb?.release()
        val rgba = frame?.rgba() ?: return Mat()

        center.x = rgba.cols() / 2.0
        center.y = rgba.rows() / 2.0

        if (dstRgb == null) {
            m = Imgproc.getRotationMatrix2D(center, 270.0, 1.0)
            dstRgb = Mat(rgba.cols(), rgba.rows(), rgba.type())
            size = Size(rgba.cols().toDouble(), rgba.rows().toDouble())
        }
        //旋转原彩图
        Imgproc.warpAffine(rgba, dstRgb, m, size)

        val rgb = dstRgb!!

        val result = wxQr.detectAndDecode(rgb)
        if (result.isNotEmpty()) {
            listener?.onQrCode(result)
        }

        return dstRgb ?: Mat()
    }

    fun interface OnQrCodeListener {
        fun onQrCode(code: List<String>)
    }
}