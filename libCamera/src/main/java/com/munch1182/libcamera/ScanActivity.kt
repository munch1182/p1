package com.munch1182.libcamera

import android.os.Bundle
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

class ScanActivity : AppCompatActivity() {

    private lateinit var lfpcp: ListenableFuture<ProcessCameraProvider>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_scan)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        kotlin.runCatching { initCamera() }
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