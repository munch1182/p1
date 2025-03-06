package com.munch1182.libcamera2

import android.Manifest
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.client.android.Intents
import com.google.zxing.common.HybridBinarizer
import com.journeyapps.barcodescanner.CaptureManager
import com.munch1182.lib.base.asPermissionCheck
import com.munch1182.lib.base.toast
import com.munch1182.libCamera2.R
import com.munch1182.libCamera2.databinding.ActivityQrCodeScanBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

class QrCodeScanActivity : AppCompatActivity() {
    private val binding: ActivityQrCodeScanBinding by lazy {
        ActivityQrCodeScanBinding.inflate(layoutInflater)
    }
    private lateinit var capture: CaptureManager
    private lateinit var photo: ImageSelect

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { _, insets ->
            val s = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.previewTitle.setPadding(s.left, s.top, s.right, s.bottom)
            insets
        }

        photo = ImageSelect(this) {
            if (it == null) {
                toast("图片选择失败")
                return@ImageSelect
            }
            decode(it)
        }
        lifecycle.addObserver(photo)

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
        binding.previewPhotoView.setOnClickListener { photo.selectImage() }
    }

    private fun decode(path: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val opt = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(path, opt)
            opt.inSampleSize = calculateInSampleSize(opt, 1024, 1024)
            opt.inJustDecodeBounds = false
            val bitmap = BitmapFactory.decodeFile(path, opt)


            val pW = bitmap.width
            val pH = bitmap.height
            val pix = IntArray(pW * pH)
            bitmap.getPixels(pix, 0, pW, 0, 0, pW, pH)

            val l = RGBLuminanceSource(pW, pH, pix)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(l))
            val text = MultiFormatReader().decode(binaryBitmap).text

            setResultIntent(text)
            finish()
        }
    }

    private fun setResultIntent(text: String?) {
        val intent = Intent(Intents.Scan.ACTION)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET)
        intent.putExtra(Intents.Scan.RESULT, text)
        setResult(RESULT_OK, intent)
    }

    private fun calculateInSampleSize(opt: BitmapFactory.Options, reqW: Int, reqH: Int): Int {
        val h = opt.outHeight
        val w = opt.outWidth
        var inSampleSize = 1
        if (h > reqH || w > reqW) {
            val halfH = h / 2
            val halfW = w / 2
            while ((halfH / inSampleSize) >= reqH && (halfW / inSampleSize) >= reqW) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
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

// https://developer.android.com/training/data-storage/shared/photopicker?hl=zh-cn
class ImageSelect(
    private val context: ComponentActivity,
    private val listener: (path: String?) -> Unit
) : DefaultLifecycleObserver {

    private lateinit var getContent: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var requestPermissCompat: ActivityResultLauncher<Array<String>>
    private val mNextLocalRequestCode = AtomicInteger()

    override fun onCreate(owner: LifecycleOwner) {
        getContent = context.activityResultRegistry.register(
            "com.aa.ImageSelect#${mNextLocalRequestCode.getAndIncrement()}",
            owner,
            ActivityResultContracts.PickVisualMedia()
        ) {
            val path = it?.let { uri -> getRealPathFromURI(context, uri) }
            listener.invoke(path)
        }
        requestPermissCompat = context.activityResultRegistry.register(
            "com.aa.ImageSelect#${mNextLocalRequestCode.getAndIncrement()}",
            owner, ActivityResultContracts.RequestMultiplePermissions()
        ) {
            if (it.any { e -> e.value }) {
                launch()
            }
        }
    }

    fun selectImage() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (!checkAll(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                )
            ) {
                requestPermissCompat.launch(
                    arrayOf(
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                    )
                )
                return
            }

        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!Manifest.permission.READ_MEDIA_IMAGES.asPermissionCheck()) {
                requestPermissCompat.launch(arrayOf(Manifest.permission.READ_MEDIA_IMAGES))
                return
            }
        }
        launch()
    }

    private fun launch() {
        getContent.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
}

private fun checkAll(vararg permissions: String): Boolean {
    return permissions.all { it.asPermissionCheck() }
}

