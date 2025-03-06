package com.munch1182.libcamera2

import android.hardware.Camera
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCallback
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions


fun ComponentActivity.registerScan(call: ActivityResultCallback<ScanIntentResult>) =
    registerForActivityResult(ScanContract(), call)

object CameraHelper {

    fun intent(): ScanOptions {
        return ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setCaptureActivity(QrCodeScanActivity::class.java)
            setOrientationLocked(true)
            setCameraId(Camera.CameraInfo.CAMERA_FACING_BACK)
            setPrompt(null)
            setBeepEnabled(false)
            setCameraId(0)
            setBarcodeImageEnabled(false)
        }
    }
}