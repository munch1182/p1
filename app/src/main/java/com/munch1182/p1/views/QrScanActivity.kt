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
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.munch1182.lib.helper.dialog.onResult
import com.munch1182.lib.helper.result.contract
import com.munch1182.lib.helper.result.onGranted
import com.munch1182.lib.helper.result.permission
import com.munch1182.lib.scan.QrScanFragment
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
    private val frag by lazy { QrScanFragment() }
    private val qrListener = QrScanHelper.OnQrCodeListener { showResult(it) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind.photo.setOnClickListener {
            contract(ActivityResultContracts.PickVisualMedia()).input(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)).request {
                it ?: return@request
                val result = frag.detectAndDecode(it)
                if (result.isEmpty()) {
                    toast("识别失败")
                    return@request
                }
                showResult(result)
            }
        }
        frag.setQrCodeListener(qrListener)
        permission(Manifest.permission.CAMERA).onGranted {
            supportFragmentManager.commit { replace(bind.fragment.id, frag) }
        }
    }

    private fun showResult(qr: List<String>) {
        lifecycleScope.launch(Dispatchers.Main) {
            frag.setQrCodeListener(null)
            DialogHelper.newBottom { v, _ ->
                ComposeView(v) {
                    Text(
                        qr.joinToString("\n"), modifier = Modifier
                            .fillMaxHeight()
                            .padding(16.dp)
                    )
                }
            }.onResult { frag.setQrCodeListener(qrListener) }.show()
        }
    }
}