package com.munch1182.lib.scan

import android.util.Log
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.opencv.wechat_qrcode.WeChatQRCode

class CameraFrameHelper {

    private var dstRgb: Mat? = null
    private var m: Mat? = null
    private val center = Point()
    private var size: Size? = null
    private val wxQr = WeChatQRCode()
    private var listener: OnQrCodeListener? = null

    fun setOnQrCodeListener(listener: OnQrCodeListener): CameraFrameHelper {
        this.listener = listener
        return this
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
        Log.d("QrScanFragment", "onCameraFrame: ${result.joinToString()}")
        if (result.isNotEmpty()) {
            listener?.onQrCode(result)
        }

        return dstRgb ?: Mat()
    }

    fun interface OnQrCodeListener {
        fun onQrCode(code: List<String>)
    }
}