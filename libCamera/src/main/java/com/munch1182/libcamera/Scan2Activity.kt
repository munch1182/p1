package com.munch1182.libcamera

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.common.util.concurrent.ListenableFuture
import com.munch1182.libCamera.R

class Scan2Activity : AppCompatActivity() {

    private lateinit var lfpcp: ListenableFuture<ProcessCameraProvider>

    override fun onCreate(savedInstanceState: Bundle?) {
        kotlin.runCatching { initCamera() }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_scan)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { _, insets ->
            val s = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            findViewById<View>(R.id.previewTitleBar).setPadding(
                s.left, s.top, s.right, s.bottom
            )
            insets
        }

    }

    private fun initCamera() {
        lfpcp = ProcessCameraProvider.getInstance(this)
        lfpcp.addListener(
            { kotlin.runCatching { bindPreview(lfpcp.get()) } },
            ContextCompat.getMainExecutor(this)
        )
    }

    private fun bindPreview(pcp: ProcessCameraProvider?) {
        val preview = Preview.Builder().build()

        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

        preview.surfaceProvider = findViewById<PreviewView>(R.id.previewView).surfaceProvider

        var camera = pcp?.bindToLifecycle(this, cameraSelector, preview)
    }
}