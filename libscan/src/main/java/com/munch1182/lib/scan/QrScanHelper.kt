package com.munch1182.lib.scan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.munch1182.lib.AppHelper
import com.munch1182.lib.base.ThreadHelper

/**
 * 使用:
 *
 * <androidx.camera.view.PreviewView
 *    android:id="@+id/preview"
 *    android:layout_width="match_parent"
 *    android:layout_height="match_parent" />
 */
class QrScanHelper {

    private var callback: OnQrCodeListener? = null
    private val scanner = BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build())
    private val ctx: Context get() = AppHelper

    /**
     * 绑定预览并解析二维码
     */
    @OptIn(ExperimentalGetImage::class)
    fun bindPreviewView(owner: LifecycleOwner, previewView: PreviewView) {
        previewView.post {
            previewView.controller = LifecycleCameraController(ctx).apply {
                bindToLifecycle(owner)
                cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                setImageAnalysisAnalyzer(ThreadHelper.cacheExecutor) { proxy ->
                    val img = proxy.image
                    if (img == null) {
                        proxy.close()
                        return@setImageAnalysisAnalyzer
                    }
                    val input = InputImage.fromMediaImage(img, proxy.imageInfo.rotationDegrees)
                    scanner.process(input).addOnSuccessListener { i ->
                        val result = i.filter { ii -> ii.format == Barcode.FORMAT_QR_CODE }.mapNotNull { ii -> ii.rawValue }
                        if (result.isNotEmpty()) callback?.onQrCode(result)
                        proxy.close()
                    }.addOnFailureListener { proxy.close() }
                }
            }
        }
    }

    fun setQrCodeListener(listener: OnQrCodeListener?): QrScanHelper {
        callback = listener
        return this
    }

    /**
     * 解析图片
     */
    fun detectAndDecode(uri: Uri, l: OnQrCodeListener) {
        try {
            val bitmap = ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } ?: return
            detectAndDecode(bitmap, l)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 解析图片
     */
    fun detectAndDecode(bitmap: Bitmap, l: OnQrCodeListener) {
        scanner.process(InputImage.fromBitmap(bitmap, 0)).addOnSuccessListener {
            val result = it.filter { ii -> ii.format == Barcode.FORMAT_QR_CODE }.mapNotNull { ii -> ii.rawValue }
            l.onQrCode(result)
        }.addOnFailureListener {
            l.onQrCode(emptyList())
        }
    }

    /**
     * 解析图片
     */
    fun interface OnQrCodeListener {
        fun onQrCode(qrCode: List<String>)
    }
}