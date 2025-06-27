package com.munch1182.lib.scan

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.hardware.Camera.PreviewCallback
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.ViewGroup
import org.opencv.BuildConfig
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import kotlin.math.min

/**
 * This class is an implementation of the Bridge View between OpenCV and Java Camera.
 * This class relays on the functionality available in base class and only implements
 * required functions:
 * connectCamera - opens Java camera and sets the PreviewCallback to be delivered.
 * disconnectCamera - closes the camera and stops preview.
 * When frame is delivered via callback from Camera - it processed via OpenCV to be
 * converted to RGBA32 and then passed to the external callback for modifications if required.
 */
class JavaCameraViewNew : CameraBridgeViewBaseNew, PreviewCallback {
    private var mBuffer: ByteArray = byteArrayOf()
    private var mFrameChain: Array<Mat?>? = null
    private var mChainIdx = 0
    private var mThread: Thread? = null
    private var mStopThread = false

    protected var mCamera: Camera? = null
    protected var mCameraFrame: Array<JavaCameraFrame?>? = null
    private var mSurfaceTexture: SurfaceTexture? = null
    override var mPreviewFormat: Int = ImageFormat.NV21

    class JavaCameraSizeAccessor : ListItemAccessor {

        override fun getWidth(obj: Any?): Int {
            val size = obj as Camera.Size
            return size.width
        }

        override fun getHeight(obj: Any?): Int {
            val size = obj as Camera.Size
            return size.height
        }
    }

    constructor(context: Context?, cameraId: Int) : super(context, cameraId)

    constructor(context: Context?, attrs: AttributeSet) : super(context, attrs)

    protected fun initializeCamera(width: Int, height: Int): Boolean {
        Log.d(TAG, "Initialize java camera")
        var result = true
        synchronized(this) {
            mCamera = null
            if (mCameraIndex == CAMERA_ID_ANY) {
                Log.d(TAG, "Trying to open camera with old open()")
                try {
                    mCamera = Camera.open()
                } catch (e: Exception) {
                    Log.e(TAG, "Camera is not available (in use or does not exist): " + e.localizedMessage)
                }

                if (mCamera == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                    var connected = false
                    for (camIdx in 0..<Camera.getNumberOfCameras()) {
                        Log.d(TAG, "Trying to open camera with new open($camIdx)")
                        try {
                            mCamera = Camera.open(camIdx)
                            connected = true
                        } catch (e: RuntimeException) {
                            Log.e(TAG, "Camera #" + camIdx + "failed to open: " + e.localizedMessage)
                        }
                        if (connected) break
                    }
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                    var localCameraIndex = mCameraIndex
                    if (mCameraIndex == CAMERA_ID_BACK) {
                        Log.i(TAG, "Trying to open back camera")
                        val cameraInfo = CameraInfo()
                        for (camIdx in 0..<Camera.getNumberOfCameras()) {
                            Camera.getCameraInfo(camIdx, cameraInfo)
                            if (cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK) {
                                localCameraIndex = camIdx
                                break
                            }
                        }
                    } else if (mCameraIndex == CAMERA_ID_FRONT) {
                        Log.i(TAG, "Trying to open front camera")
                        val cameraInfo = CameraInfo()
                        for (camIdx in 0..<Camera.getNumberOfCameras()) {
                            Camera.getCameraInfo(camIdx, cameraInfo)
                            if (cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
                                localCameraIndex = camIdx
                                break
                            }
                        }
                    }
                    if (localCameraIndex == CAMERA_ID_BACK) {
                        Log.e(TAG, "Back camera not found!")
                    } else if (localCameraIndex == CAMERA_ID_FRONT) {
                        Log.e(TAG, "Front camera not found!")
                    } else {
                        Log.d(TAG, "Trying to open camera with new open($localCameraIndex)")
                        try {
                            mCamera = Camera.open(localCameraIndex)
                        } catch (e: RuntimeException) {
                            Log.e(TAG, "Camera #" + localCameraIndex + "failed to open: " + e.localizedMessage)
                        }
                    }
                }
            }

            if (mCamera == null) return false

            /* Now set camera parameters */

            val mCamera: Camera = mCamera!!
            try {
                var params = mCamera.getParameters()
                Log.d(TAG, "getSupportedPreviewSizes()")
                val sizes = params.supportedPreviewSizes

                if (sizes != null) {
                    /* Select the size that fits surface considering maximum size allowed */
                    val frameSize = calculateCameraFrameSize(sizes, JavaCameraSizeAccessor(), width, height)

                    /* Image format NV21 causes issues in the Android emulators */
                    if (Build.FINGERPRINT.startsWith("generic")
                        || Build.FINGERPRINT.startsWith("unknown")
                        || Build.MODEL.contains("google_sdk")
                        || Build.MODEL.contains("Emulator")
                        || Build.MODEL.contains("Android SDK built for x86")
                        || Build.MANUFACTURER.contains("Genymotion")
                        || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                        || "google_sdk" == Build.PRODUCT
                    ) params.previewFormat = ImageFormat.YV12 // "generic" or "android" = android emulator
                    else params.previewFormat = ImageFormat.NV21

                    mPreviewFormat = params.previewFormat

                    Log.d(TAG, "Set preview size to " + frameSize.width.toInt() + "x" + frameSize.height.toInt())
                    params.setPreviewSize(frameSize.width.toInt(), frameSize.height.toInt())

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH && Build.MODEL != "GT-I9100") params.setRecordingHint(true)

                    val FocusModes = params.supportedFocusModes
                    if (FocusModes != null && FocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                        params.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO
                    }

                    mCamera.setParameters(params)
                    params = mCamera.getParameters()

                    mFrameWidth = params.previewSize.width
                    mFrameHeight = params.previewSize.height

                    mScale = if ((layoutParams.width == ViewGroup.LayoutParams.MATCH_PARENT) && (layoutParams.height == ViewGroup.LayoutParams.MATCH_PARENT)) min(((height.toFloat()) / mFrameHeight).toDouble(), ((width.toFloat()) / mFrameWidth).toDouble()).toFloat()
                    else 0f

                    if (mFpsMeter != null) {
                        mFpsMeter!!.setResolution(mFrameWidth, mFrameHeight)
                    }

                    var size = mFrameWidth * mFrameHeight
                    size = size * ImageFormat.getBitsPerPixel(params.previewFormat) / 8
                    mBuffer = ByteArray(size)

                    mCamera.addCallbackBuffer(mBuffer)
                    mCamera.setPreviewCallbackWithBuffer(this)

                    mFrameChain = arrayOfNulls(2)
                    mFrameChain!![0] = Mat(mFrameHeight + (mFrameHeight / 2), mFrameWidth, CvType.CV_8UC1)
                    mFrameChain!![1] = Mat(mFrameHeight + (mFrameHeight / 2), mFrameWidth, CvType.CV_8UC1)

                    AllocateCache()

                    mCameraFrame = arrayOfNulls(2)
                    mCameraFrame!![0] = JavaCameraFrame(mFrameChain!![0], mFrameWidth, mFrameHeight)
                    mCameraFrame!![1] = JavaCameraFrame(mFrameChain!![1], mFrameWidth, mFrameHeight)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                        mSurfaceTexture = SurfaceTexture(MAGIC_TEXTURE_ID)
                        mCamera.setPreviewTexture(mSurfaceTexture)
                    } else mCamera.setPreviewDisplay(null)

                    /* Finally we are ready to start the preview */
                    Log.d(TAG, "startPreview")
                    mCamera.startPreview()
                } else result = false
            } catch (e: Exception) {
                result = false
                e.printStackTrace()
            }
        }

        return result
    }

    protected fun releaseCamera() {
        synchronized(this) {
            if (mCamera != null) {
                mCamera!!.stopPreview()
                mCamera!!.setPreviewCallback(null)

                mCamera!!.release()
            }
            mCamera = null
            if (mFrameChain != null) {
                mFrameChain!![0]!!.release()
                mFrameChain!![1]!!.release()
            }
            if (mCameraFrame != null) {
                mCameraFrame!![0]!!.release()
                mCameraFrame!![1]!!.release()
            }
        }
    }

    private var mCameraFrameReady = false

    override fun connectCamera(width: Int, height: Int): Boolean {
        /* 1. We need to instantiate camera
                * 2. We need to start thread which will be getting frames
                */
        /* First step - initialize camera connection */

        Log.d(TAG, "Connecting to camera")
        if (!initializeCamera(width, height)) return false

        mCameraFrameReady = false

        /* now we can start update thread */
        Log.d(TAG, "Starting processing thread")
        mStopThread = false
        mThread = Thread(CameraWorker())
        mThread!!.start()

        return true
    }

    override fun disconnectCamera() {
        /* 1. We need to stop thread which updating the frames
         * 2. Stop camera and release it
         */
        Log.d(TAG, "Disconnecting from camera")
        try {
            mStopThread = true
            Log.d(TAG, "Notify thread")
            synchronized(this) {
                (this as Object).notify()
            }
            Log.d(TAG, "Waiting for thread")
            if (mThread != null) mThread!!.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        } finally {
            mThread = null
        }

        /* Now release camera */
        releaseCamera()

        mCameraFrameReady = false
    }

    override fun onPreviewFrame(frame: ByteArray, arg1: Camera) {
        if (BuildConfig.DEBUG) Log.d(TAG, "Preview Frame received. Frame size: " + frame.size)
        synchronized(this) {
            mFrameChain!![mChainIdx]!!.put(0, 0, frame)
            mCameraFrameReady = true
            (this as Object).notify()
        }
        if (mCamera != null) mCamera!!.addCallbackBuffer(mBuffer)
    }

    inner class JavaCameraFrame(private val mYuvFrameData: Mat?, private val mWidth: Int, private val mHeight: Int) : CvCameraViewFrame {
        override fun gray(): Mat {
            return mYuvFrameData!!.submat(0, mHeight, 0, mWidth)
        }

        override fun rgba(): Mat {
            if (mPreviewFormat == ImageFormat.NV21) Imgproc.cvtColor(mYuvFrameData, mRgba, Imgproc.COLOR_YUV2RGBA_NV21, 4)
            else if (mPreviewFormat == ImageFormat.YV12) Imgproc.cvtColor(mYuvFrameData, mRgba, Imgproc.COLOR_YUV2RGB_I420, 4) // COLOR_YUV2RGBA_YV12 produces inverted colors
            else throw IllegalArgumentException("Preview Format can be NV21 or YV12")

            return mRgba
        }

        fun release() {
            mRgba.release()
        }

        private val mRgba = Mat()
    }

    private inner class CameraWorker : Runnable {
        override fun run() {
            do {
                var hasFrame = false
                synchronized(this@JavaCameraViewNew) {
                    try {
                        while (!mCameraFrameReady && !mStopThread) {
                            (this@JavaCameraViewNew as Object).wait()
                        }
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                    if (mCameraFrameReady) {
                        mChainIdx = 1 - mChainIdx
                        mCameraFrameReady = false
                        hasFrame = true
                    }
                }

                if (!mStopThread && hasFrame) {
                    if (!mFrameChain!![1 - mChainIdx]!!.empty()) deliverAndDrawFrame(mCameraFrame!![1 - mChainIdx]!!)
                }
            } while (!mStopThread)
            Log.d(TAG, "Finish processing thread")
        }
    }

    companion object {
        private const val MAGIC_TEXTURE_ID = 10
        private const val TAG = "JavaCameraView"
    }
}