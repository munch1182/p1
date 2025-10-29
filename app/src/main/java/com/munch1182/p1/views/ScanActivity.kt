package com.munch1182.p1.views

import android.Manifest
import android.os.Bundle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.munch1182.lib.base.isGranted
import com.munch1182.lib.base.shareTextIntent
import com.munch1182.lib.helper.onResult
import com.munch1182.lib.helper.result.intent
import com.munch1182.lib.helper.result.isAllGranted
import com.munch1182.lib.helper.result.manualIntent
import com.munch1182.lib.helper.result.permission
import com.munch1182.lib.scan.QrScanHelper
import com.munch1182.p1.base.BaseActivity
import com.munch1182.p1.base.DialogHelper
import com.munch1182.p1.base.bind
import com.munch1182.p1.base.onPermission
import com.munch1182.p1.databinding.ActivityScanBinding
import com.munch1182.p1.ui.RvPage
import com.munch1182.p1.ui.theme.PagePadding

class ScanActivity : BaseActivity(), QrScanHelper.OnQrCodeListener {

    private val scan by lazy { QrScanHelper() }
    private val bind by bind(ActivityScanBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scan.setQrCodeListener(this)
        bind.preview.setOnClickListener { if (!Manifest.permission.CAMERA.isGranted()) request() }
        request()
    }

    private fun request() {
        permission(Manifest.permission.CAMERA)
            .onPermission("相机" to "识别二维码")
            .manualIntent()
            .request {
                if (it.isAllGranted()) {
                    scan.bindPreviewView(this, bind.preview)
                }
            }
    }

    override fun onQrCode(qrCode: List<String>) {
        DialogHelper.newBottom {
            scan.setQrCodeListener(null)
            RvPage(qrCode.toTypedArray(), modifier = Modifier.heightIn(min = 400.dp)) { s ->
                Text(
                    s, Modifier
                        .clickable { intent(shareTextIntent(s)).request {} }
                        .padding(PagePadding))
            }
        }.onResult { scan.setQrCodeListener(this) }
            .show()
    }

}