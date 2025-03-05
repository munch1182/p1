package com.munch1182.libcamera2

import android.hardware.Camera
import androidx.activity.result.ActivityResultCallback
import androidx.fragment.app.FragmentActivity
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions


fun FragmentActivity.registerScan(call: ActivityResultCallback<ScanIntentResult>) =
    registerForActivityResult(ScanContract(), call)

object CameraHelper {

    fun intent(): ScanOptions {
        return ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setCaptureActivity(QrCodeScanActivity::class.java)
            setOrientationLocked(true)
            setCameraId(Camera.CameraInfo.CAMERA_FACING_FRONT)
            setPrompt(null)
            setBeepEnabled(false)
            setCameraId(0)
            setBarcodeImageEnabled(false)
        }
    }
}