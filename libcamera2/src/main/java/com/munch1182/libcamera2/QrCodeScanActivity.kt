package com.munch1182.libcamera2

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.journeyapps.barcodescanner.CaptureManager
import com.munch1182.libCamera2.R
import com.munch1182.libCamera2.databinding.ActivityQrCodeScanBinding

class QrCodeScanActivity : AppCompatActivity() {
    private val binding: ActivityQrCodeScanBinding by lazy {
        ActivityQrCodeScanBinding.inflate(layoutInflater)
    }
    private lateinit var capture: CaptureManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { _, insets ->
            val s = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.previewTitle.setPadding(s.left, s.top, s.right, s.bottom)
            insets
        }

        capture = CaptureManager(this, binding.previewDBV)
        capture.initializeFromIntent(intent, savedInstanceState)
        capture.setShowMissingCameraPermissionDialog(true)
        capture.decode()

        binding.previewTorch.setOnClickListener {
            val selected = !binding.previewTorch.isSelected
            binding.previewTorch.isSelected = selected
            if (selected) {
                binding.previewDBV.setTorchOn()
            } else {
                binding.previewDBV.setTorchOff()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        capture.onResume()
    }

    override fun onPause() {
        super.onPause()
        capture.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        capture.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        capture.onSaveInstanceState(outState)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        capture.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}