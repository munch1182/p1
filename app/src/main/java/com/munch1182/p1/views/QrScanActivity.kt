package com.munch1182.p1.views

import android.Manifest
import android.os.Bundle
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.munch1182.lib.helper.dialog.onResult
import com.munch1182.lib.helper.result.contract
import com.munch1182.lib.helper.result.onGranted
import com.munch1182.lib.helper.result.permission
import com.munch1182.lib.scan.QrScanHelper
import com.munch1182.p1.base.BaseActivity
import com.munch1182.p1.base.DialogHelper
import com.munch1182.p1.base.bind
import com.munch1182.p1.base.toast
import com.munch1182.p1.databinding.ActivityQrScanBinding
import com.munch1182.p1.ui.ComposeView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class QrScanActivity : BaseActivity() {

    private val bind by bind(ActivityQrScanBinding::inflate)
    private val qrListener = QrScanHelper.OnQrCodeListener { showResult(it) }
    private val helper by lazy { QrScanHelper().setQrCodeListener(qrListener) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind.photo.setOnClickListener {
            contract(ActivityResultContracts.PickVisualMedia()).input(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)).request {
                it ?: return@request
                helper.detectAndDecode(it, qrListener)
            }
        }
        withPermission { helper.bindPreviewView(this, bind.preview) }
    }

    private fun withPermission(any: () -> Unit) {
        permission(Manifest.permission.CAMERA).onGranted(any)
    }

    private fun showResult(qr: List<String>) {
        if (qr.isEmpty()) {
            toast("识别失败")
            return
        }
        lifecycleScope.launch(Dispatchers.Main) {
            helper.setQrCodeListener(null)
            DialogHelper.newBottom { v, _ ->
                ComposeView(v) {
                    Text(
                        qr.joinToString("\n"), modifier = Modifier
                            .fillMaxHeight()
                            .padding(16.dp)
                    )
                }
            }.onResult { helper.setQrCodeListener(qrListener) }.show()
        }
    }
}