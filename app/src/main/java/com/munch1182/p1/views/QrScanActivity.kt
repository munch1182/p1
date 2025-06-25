package com.munch1182.p1.views

import android.os.Bundle
import android.view.View
import com.munch1182.lib.base.log
import com.munch1182.p1.base.bind
import com.munch1182.p1.base.toast
import com.munch1182.p1.databinding.ActivityQrScanBinding
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.CameraActivity
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.opencv.wechat_qrcode.WeChatQRCode

class QrScanActivity : CameraActivity() {

    private val bind by bind(ActivityQrScanBinding::inflate)
    private val wxQr by lazy { WeChatQRCode() }

    private val points = mutableListOf<Mat>()
    private val scalar = Scalar(255.0, 255.0, 0.0, 0.0)
    private val center = Point()

    private var dstRgb: Mat? = null
    private var dstGray: Mat? = null
    private var m: Mat? = null
    private var size: Size? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bind.tutorial1ActivityJavaSurfaceView.visibility = View.VISIBLE
        bind.tutorial1ActivityJavaSurfaceView.setCvCameraViewListener(object : CameraBridgeViewBase.CvCameraViewListener2 {
            override fun onCameraViewStarted(width: Int, height: Int) {
            }

            override fun onCameraViewStopped() {
            }

            override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame?): Mat {
                points.clear()
                dstGray?.release()
                dstRgb?.release()

                val rgba = inputFrame?.rgba() ?: return Mat()
                val grayMat = inputFrame.gray() ?: return Mat()

                center.x = rgba.cols() / 2.0
                center.y = rgba.rows() / 2.0

                if (dstRgb == null) {
                    m = Imgproc.getRotationMatrix2D(center, 270.0, 1.0)
                    dstRgb = Mat(rgba.cols(), rgba.rows(), rgba.type())
                    dstGray = Mat(rgba.cols(), rgba.rows(), rgba.type())
                    size = Size(rgba.cols().toDouble(), rgba.rows().toDouble())
                }

                Imgproc.warpAffine(rgba, dstRgb, m, size)
                Imgproc.warpAffine(grayMat, dstGray, m, size)

                val result = wxQr.detectAndDecode(dstRgb, points)

                val code = result?.joinToString()

                log().logStr("code: $code")
                if (!code.isNullOrEmpty()) {
                    runOnUiThread { toast(code) }
                }

                return dstRgb!!
            }
        })
    }

    override fun onResume() {
        super.onResume()

        val initDebug = OpenCVLoader.initDebug()
        log().logStr("initDebug: $initDebug")
        if (!initDebug) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, loadCallback)
        } else {
            loadCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
    }

    private val loadCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            if (status == LoaderCallbackInterface.SUCCESS) {
                bind.tutorial1ActivityJavaSurfaceView.enableView()
            } else {
                super.onManagerConnected(status)
            }
        }
    }

    override fun getCameraViewList(): MutableList<out CameraBridgeViewBase> {
        return mutableListOf(bind.tutorial1ActivityJavaSurfaceView)
    }

    override fun onPause() {
        super.onPause()
        bind.tutorial1ActivityJavaSurfaceView.disableView()
    }
}