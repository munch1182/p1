package com.munch1182.lib.scan

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import org.opencv.android.InstallCallbackInterface
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.core.Mat

class QrScanFragment : Fragment() {

    private var camera: CameraBridgeViewBaseNew? = null
    private val loadCallback = object : LoaderCallbackInterface {
        override fun onManagerConnected(status: Int) {
            if (status == LoaderCallbackInterface.SUCCESS) {
                camera?.enableView()
            }
        }

        override fun onPackageInstall(operation: Int, callback: InstallCallbackInterface?) {
        }
    }
    private val cameraListener = object : CameraBridgeViewBaseNew.CvCameraViewListener2 {
        override fun onCameraViewStarted(width: Int, height: Int) {
        }

        override fun onCameraViewStopped() {
        }

        override fun onCameraFrame(inputFrame: CameraBridgeViewBaseNew.CvCameraViewFrame?): Mat {
            return cameraHelper.onCameraFrame(inputFrame)
        }
    }
    private val cameraHelper by lazy {
        CameraFrameHelper().setOnQrCodeListener {
            Log.d("QrScanFragment", "code:  ${it.joinToString()} ")
            activity?.runOnUiThread {
                Toast.makeText(activity, it.joinToString(), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fraqment_qr_scan, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        camera = view.findViewById(R.id.cameraView)
        camera?.setCvCameraViewListener(cameraListener)
        camera?.setCameraPermissionGranted()
    }

    override fun onPause() {
        super.onPause()
        camera?.disableView()
    }

    override fun onResume() {
        super.onResume()
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, context, loadCallback)
        } else {
            loadCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
    }
}