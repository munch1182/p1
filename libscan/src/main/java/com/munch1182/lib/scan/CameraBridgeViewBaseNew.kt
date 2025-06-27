package com.munch1182.lib.scan

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.core.content.withStyledAttributes
import org.opencv.BuildConfig
import org.opencv.R
import org.opencv.android.FpsMeter
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.Size

/**
 * This is a basic class, implementing the interaction with Camera and OpenCV library.
 * The main responsibility of it - is to control when camera can be enabled, process the frame,
 * call external listener to make any adjustments to the frame and then draw the resulting
 * frame to the screen.
 * The clients shall implement CvCameraViewListener.
 */
abstract class CameraBridgeViewBaseNew : SurfaceView, SurfaceHolder.Callback {
    private var mState: Int = STOPPED
    private var mCacheBitmap: Bitmap? = null
    private var mListener: CvCameraViewListener2? = null
    private var mSurfaceExist = false
    private val mSyncObject = Any()

    protected var mFrameWidth: Int = 0
    protected var mFrameHeight: Int = 0
    protected var mMaxHeight: Int = MAX_UNSPECIFIED
    protected var mMaxWidth: Int = MAX_UNSPECIFIED
    protected var mScale: Float = 2f // 修改1：放大做出全屏效果
    protected open var mPreviewFormat: Int = RGBA
    protected var mCameraIndex: Int = CAMERA_ID_ANY
    protected var mEnabled: Boolean = false
    protected var mCameraPermissionGranted: Boolean = false
    protected var mFpsMeter: FpsMeter? = null

    constructor(context: Context?, cameraId: Int) : super(context) {
        mCameraIndex = cameraId
        holder.addCallback(this)
        mMaxWidth = MAX_UNSPECIFIED
        mMaxHeight = MAX_UNSPECIFIED
    }

    constructor(context: Context?, attrs: AttributeSet) : super(context, attrs) {
        val count = attrs.attributeCount
        Log.d(TAG, "Attr count: $count")

        getContext().withStyledAttributes(attrs, R.styleable.CameraBridgeViewBase) {
            if (getBoolean(R.styleable.CameraBridgeViewBase_show_fps, false)) enableFpsMeter()

            mCameraIndex = getInt(R.styleable.CameraBridgeViewBase_camera_id, -1)

            holder.addCallback(this@CameraBridgeViewBaseNew)
            mMaxWidth = MAX_UNSPECIFIED
            mMaxHeight = MAX_UNSPECIFIED
        }
    }

    /**
     * Sets the camera index
     * @param cameraIndex new camera index
     */
    fun setCameraIndex(cameraIndex: Int) {
        this.mCameraIndex = cameraIndex
    }

    interface CvCameraViewListener {
        /**
         * This method is invoked when camera preview has started. After this method is invoked
         * the frames will start to be delivered to client via the onCameraFrame() callback.
         * @param width -  the width of the frames that will be delivered
         * @param height - the height of the frames that will be delivered
         */
        fun onCameraViewStarted(width: Int, height: Int)

        /**
         * This method is invoked when camera preview has been stopped for some reason.
         * No frames will be delivered via onCameraFrame() callback after this method is called.
         */
        fun onCameraViewStopped()

        /**
         * This method is invoked when delivery of the frame needs to be done.
         * The returned values - is a modified frame which needs to be displayed on the screen.
         * TODO: pass the parameters specifying the format of the frame (BPP, YUV or RGB and etc)
         */
        fun onCameraFrame(inputFrame: Mat?): Mat?
    }

    interface CvCameraViewListener2 {
        /**
         * This method is invoked when camera preview has started. After this method is invoked
         * the frames will start to be delivered to client via the onCameraFrame() callback.
         * @param width -  the width of the frames that will be delivered
         * @param height - the height of the frames that will be delivered
         */
        fun onCameraViewStarted(width: Int, height: Int)

        /**
         * This method is invoked when camera preview has been stopped for some reason.
         * No frames will be delivered via onCameraFrame() callback after this method is called.
         */
        fun onCameraViewStopped()

        /**
         * This method is invoked when delivery of the frame needs to be done.
         * The returned values - is a modified frame which needs to be displayed on the screen.
         * TODO: pass the parameters specifying the format of the frame (BPP, YUV or RGB and etc)
         */
        fun onCameraFrame(inputFrame: CvCameraViewFrame?): Mat?
    }

    protected inner class CvCameraViewListenerAdapter(private val mOldStyleListener: CvCameraViewListener) : CvCameraViewListener2 {
        override fun onCameraViewStarted(width: Int, height: Int) {
            mOldStyleListener.onCameraViewStarted(width, height)
        }

        override fun onCameraViewStopped() {
            mOldStyleListener.onCameraViewStopped()
        }

        override fun onCameraFrame(inputFrame: CvCameraViewFrame?): Mat? {
            var result: Mat? = null
            when (mPreviewFormat) {
                RGBA -> result = mOldStyleListener.onCameraFrame(inputFrame?.rgba())
                GRAY -> result = mOldStyleListener.onCameraFrame(inputFrame?.gray())
                else -> Log.e(TAG, "Invalid frame format! Only RGBA and Gray Scale are supported!")
            }



            return result
        }

        fun setFrameFormat(format: Int) {
            mPreviewFormat = format
        }

        private var mPreviewFormat: Int = RGBA
    }

    /**
     * This class interface is abstract representation of single frame from camera for onCameraFrame callback
     * Attention: Do not use objects, that represents this interface out of onCameraFrame callback!
     */
    interface CvCameraViewFrame {
        /**
         * This method returns RGBA Mat with frame
         */
        fun rgba(): Mat?

        /**
         * This method returns single channel gray scale Mat with frame
         */
        fun gray(): Mat?
    }

    override fun surfaceChanged(arg0: SurfaceHolder, arg1: Int, arg2: Int, arg3: Int) {
        Log.d(TAG, "call surfaceChanged event")
        synchronized(mSyncObject) {
            if (!mSurfaceExist) {
                mSurfaceExist = true
                checkCurrentState()
            } else {
                /** Surface changed. We need to stop camera and restart with new parameters  */
                /** Surface changed. We need to stop camera and restart with new parameters  */
                /* Pretend that old surface has been destroyed */
                mSurfaceExist = false
                checkCurrentState()
                /* Now use new surface. Say we have it now */
                mSurfaceExist = true
                checkCurrentState()
            }
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        /* Do nothing. Wait until surfaceChanged delivered */
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        synchronized(mSyncObject) {
            mSurfaceExist = false
            checkCurrentState()
        }
    }


    /**
     * This method is provided for clients, so they can signal camera permission has been granted.
     * The actual onCameraViewStarted callback will be delivered only after setCameraPermissionGranted
     * and enableView have been called and surface is available
     */
    fun setCameraPermissionGranted() {
        synchronized(mSyncObject) {
            mCameraPermissionGranted = true
            checkCurrentState()
        }
    }


    /**
     * This method is provided for clients, so they can enable the camera connection.
     * The actual onCameraViewStarted callback will be delivered only after setCameraPermissionGranted
     * and enableView have been called and surface is available
     */
    fun enableView() {
        synchronized(mSyncObject) {
            mEnabled = true
            checkCurrentState()
        }
    }

    /**
     * This method is provided for clients, so they can disable camera connection and stop
     * the delivery of frames even though the surface view itself is not destroyed and still stays on the screen
     */
    fun disableView() {
        synchronized(mSyncObject) {
            mEnabled = false
            checkCurrentState()
        }
    }

    /**
     * This method enables label with fps value on the screen
     */
    fun enableFpsMeter() {
        if (mFpsMeter == null) {
            mFpsMeter = FpsMeter()
            mFpsMeter!!.setResolution(mFrameWidth, mFrameHeight)
        }
    }

    fun disableFpsMeter() {
        mFpsMeter = null
    }

    /**
     *
     * @param listener
     */
    fun setCvCameraViewListener(listener: CvCameraViewListener2?) {
        mListener = listener
    }

    fun setCvCameraViewListener(listener: CvCameraViewListener) {
        val adapter = CvCameraViewListenerAdapter(listener)
        adapter.setFrameFormat(mPreviewFormat)
        mListener = adapter
    }

    /**
     * This method sets the maximum size that camera frame is allowed to be. When selecting
     * size - the biggest size which less or equal the size set will be selected.
     * As an example - we set setMaxFrameSize(200,200) and we have 176x152 and 320x240 sizes. The
     * preview frame will be selected with 176x152 size.
     * This method is useful when need to restrict the size of preview frame for some reason (for example for video recording)
     * @param maxWidth - the maximum width allowed for camera frame.
     * @param maxHeight - the maximum height allowed for camera frame
     */
    fun setMaxFrameSize(maxWidth: Int, maxHeight: Int) {
        mMaxWidth = maxWidth
        mMaxHeight = maxHeight
    }

    fun SetCaptureFormat(format: Int) {
        mPreviewFormat = format
        if (mListener is CvCameraViewListenerAdapter) {
            val adapter = mListener as CvCameraViewListenerAdapter
            adapter.setFrameFormat(mPreviewFormat)
        }
    }

    /**
     * Called when mSyncObject lock is held
     */
    private fun checkCurrentState() {
        Log.d(TAG, "call checkCurrentState")

        val targetState: Int = if (mEnabled && mCameraPermissionGranted && mSurfaceExist && visibility == VISIBLE) {
            STARTED
        } else {
            STOPPED
        }

        if (targetState != mState) {
            /* The state change detected. Need to exit the current state and enter target state */
            processExitState(mState)
            mState = targetState
            processEnterState(mState)
        }
    }

    private fun processEnterState(state: Int) {
        Log.d(TAG, "call processEnterState: $state")
        when (state) {
            STARTED -> {
                onEnterStartedState()
                if (mListener != null) {
                    mListener!!.onCameraViewStarted(mFrameWidth, mFrameHeight)
                }
            }

            STOPPED -> {
                onEnterStoppedState()
                if (mListener != null) {
                    mListener!!.onCameraViewStopped()
                }
            }
        }
    }

    private fun processExitState(state: Int) {
        Log.d(TAG, "call processExitState: $state")
        when (state) {
            STARTED -> onExitStartedState()
            STOPPED -> onExitStoppedState()
        }
    }

    private fun onEnterStoppedState() {
        /* nothing to do */
    }

    private fun onExitStoppedState() {
        /* nothing to do */
    }

    // NOTE: The order of bitmap constructor and camera connection is important for android 4.1.x
    // Bitmap must be constructed before surface
    private fun onEnterStartedState() {
        Log.d(TAG, "call onEnterStartedState")
        /* Connect camera */
        // 修改：提高帧数
        if (!connectCamera(640, 480)) {
            /*if (!connectCamera(width, height)) {*/
            val ad = AlertDialog.Builder(context).create()
            ad.setCancelable(false) // This blocks the 'BACK' button
            ad.setMessage("It seems that you device does not support camera (or it is locked). Application will be closed.")
            ad.setButton(DialogInterface.BUTTON_NEUTRAL, "OK") { dialog, which ->
                dialog.dismiss()
                (context as Activity).finish()
            }
            ad.show()
        }
    }

    private fun onExitStartedState() {
        disconnectCamera()
        if (mCacheBitmap != null) {
            mCacheBitmap!!.recycle()
        }
    }

    /**
     * This method shall be called by the subclasses when they have valid
     * object and want it to be delivered to external client (via callback) and
     * then displayed on the screen.
     * @param frame - the current frame to be delivered
     */
    protected fun deliverAndDrawFrame(frame: CvCameraViewFrame) {
        val modified = if (mListener != null) {
            mListener!!.onCameraFrame(frame)
        } else {
            frame.rgba()
        }

        var bmpValid = true
        if (modified != null) {
            try {
                Utils.matToBitmap(modified, mCacheBitmap)
            } catch (e: Exception) {
                Log.e(TAG, "Mat type: $modified")
                Log.e(TAG, "Bitmap type: " + mCacheBitmap!!.width + "*" + mCacheBitmap!!.height)
                Log.e(TAG, "Utils.matToBitmap() throws an exception: " + e.message)
                bmpValid = false
            }
        }

        // 修改缩放
        if (mScale != 0f) {
            if (modified!!.cols() < width || modified.rows() < height) {
                val scaleW = width / modified.cols()
                val scaleH = height / modified.rows()
                val maxScale = scaleW.coerceAtLeast(scaleH)
                mScale = maxScale.toFloat()
            }
        }

        if (bmpValid && mCacheBitmap != null) {
            val canvas = holder.lockCanvas()
            if (canvas != null) {
                canvas.drawColor(0, PorterDuff.Mode.CLEAR)
                if (BuildConfig.DEBUG) Log.d(TAG, "mStretch value: $mScale")

                if (mScale != 0f) {
                    canvas.drawBitmap(
                        mCacheBitmap!!, Rect(0, 0, mCacheBitmap!!.width, mCacheBitmap!!.height),
                        Rect(
                            ((canvas.width - mScale * mCacheBitmap!!.width) / 2).toInt(),
                            ((canvas.height - mScale * mCacheBitmap!!.height) / 2).toInt(),
                            ((canvas.width - mScale * mCacheBitmap!!.width) / 2 + mScale * mCacheBitmap!!.width).toInt(),
                            ((canvas.height - mScale * mCacheBitmap!!.height) / 2 + mScale * mCacheBitmap!!.height).toInt()
                        ), null
                    )
                } else {
                    canvas.drawBitmap(
                        mCacheBitmap!!, Rect(0, 0, mCacheBitmap!!.width, mCacheBitmap!!.height),
                        Rect(
                            (canvas.width - mCacheBitmap!!.width) / 2,
                            (canvas.height - mCacheBitmap!!.height) / 2,
                            (canvas.width - mCacheBitmap!!.width) / 2 + mCacheBitmap!!.width,
                            (canvas.height - mCacheBitmap!!.height) / 2 + mCacheBitmap!!.height
                        ), null
                    )
                }

                if (mFpsMeter != null) {
                    mFpsMeter!!.measure()
                    mFpsMeter!!.draw(canvas, 20f, 30f)
                }
                holder.unlockCanvasAndPost(canvas)
            }
        }
    }

    /**
     * This method is invoked shall perform concrete operation to initialize the camera.
     * CONTRACT: as a result of this method variables mFrameWidth and mFrameHeight MUST be
     * initialized with the size of the Camera frames that will be delivered to external processor.
     * @param width - the width of this SurfaceView
     * @param height - the height of this SurfaceView
     */
    protected abstract fun connectCamera(width: Int, height: Int): Boolean

    /**
     * Disconnects and release the particular camera object being connected to this surface view.
     * Called when syncObject lock is held
     */
    protected abstract fun disconnectCamera()

    // NOTE: On Android 4.1.x the function must be called before SurfaceTexture constructor!
    protected fun AllocateCache() {
        mCacheBitmap = Bitmap.createBitmap(mFrameWidth, mFrameHeight, Bitmap.Config.ARGB_8888)
    }

    interface ListItemAccessor {
        fun getWidth(obj: Any?): Int
        fun getHeight(obj: Any?): Int
    }

    /**
     * This helper method can be called by subclasses to select camera preview size.
     * It goes over the list of the supported preview sizes and selects the maximum one which
     * fits both values set via setMaxFrameSize() and surface frame allocated for this view
     * @param supportedSizes
     * @param surfaceWidth
     * @param surfaceHeight
     * @return optimal frame size
     */
    protected fun calculateCameraFrameSize(supportedSizes: List<*>, accessor: ListItemAccessor, surfaceWidth: Int, surfaceHeight: Int): Size {
        var calcWidth = 0
        var calcHeight = 0

        val maxAllowedWidth = if (mMaxWidth != MAX_UNSPECIFIED && mMaxWidth < surfaceWidth) mMaxWidth else surfaceWidth
        val maxAllowedHeight = if (mMaxHeight != MAX_UNSPECIFIED && mMaxHeight < surfaceHeight) mMaxHeight else surfaceHeight

        for (size in supportedSizes) {
            val width = accessor.getWidth(size)
            val height = accessor.getHeight(size)
            Log.d(TAG, "trying size: " + width + "x" + height)

            if (width <= maxAllowedWidth && height <= maxAllowedHeight) {
                if (width >= calcWidth && height >= calcHeight) {
                    calcWidth = width
                    calcHeight = height
                }
            }
        }
        if ((calcWidth == 0 || calcHeight == 0) && supportedSizes.size > 0) {
            Log.i(TAG, "fallback to the first frame size")
            val size = supportedSizes[0]!!
            calcWidth = accessor.getWidth(size)
            calcHeight = accessor.getHeight(size)
        }

        return Size(calcWidth.toDouble(), calcHeight.toDouble())
    }

    companion object {
        private const val TAG = "CameraBridge"
        protected const val MAX_UNSPECIFIED: Int = -1
        private const val STOPPED = 0
        private const val STARTED = 1

        const val CAMERA_ID_ANY: Int = -1
        const val CAMERA_ID_BACK: Int = 99
        const val CAMERA_ID_FRONT: Int = 98
        const val RGBA: Int = 1
        const val GRAY: Int = 2
    }
}