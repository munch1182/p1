package com.munch1182.p1.views

import android.Manifest
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import com.munch1182.android.lib.helper.onResult
import com.munch1182.android.lib.helper.result.permission
import com.munch1182.android.scan.QrScanHelper
import com.munch1182.p1.base.BaseActivity
import com.munch1182.p1.base.DialogHelper
import com.munch1182.p1.base.appIntent
import com.munch1182.p1.base.bind
import com.munch1182.p1.base.onDialog
import com.munch1182.p1.databinding.ActivityQrScanBinding
import com.munch1182.p1.ui.RvPage
import com.munch1182.p1.ui.theme.PagePaddingModifier

class QrScanActivity : BaseActivity() {

    private val bind by bind(ActivityQrScanBinding::inflate)
    private val helper by lazy { QrScanHelper() }
    private val qrCodeListener = object : QrScanHelper.OnQrCodeListener {
        override fun onQrCode(qrCode: List<String>) {
            if (qrCode.isNotEmpty()) {
                helper.setQrCodeListener(null)
                DialogHelper.newBottom {
                    RvPage(qrCode.toTypedArray(), PagePaddingModifier.defaultMinSize(minHeight = 500.dp)) {
                        Text(it)
                    }
                }.onResult { helper.setQrCodeListener(this) }.show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        fitWindow(bind.open)
        permission(listOf(Manifest.permission.CAMERA))
            .onDialog("相机", "扫描、识别二维码")
            .appIntent()
            .request {
                if (it.values.all { p -> p.isGranted }) {
                    bind.preview.post {
                        helper.bindPreviewView(this, bind.preview)
                        helper.setQrCodeListener(qrCodeListener)
                    }
                }
            }

    }
}