package com.munch1182.p1.views

import android.Manifest
import android.os.Bundle
import androidx.fragment.app.commit
import com.munch1182.lib.helper.result.onGranted
import com.munch1182.lib.helper.result.permission
import com.munch1182.lib.scan.QrScanFragment
import com.munch1182.p1.base.BaseActivity
import com.munch1182.p1.base.bind
import com.munch1182.p1.databinding.ActivityQrScanBinding

class QrScanActivity : BaseActivity() {

    private val bind by bind(ActivityQrScanBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permission(Manifest.permission.CAMERA).onGranted {
            supportFragmentManager.commit { replace(bind.fragment.id, QrScanFragment()) }
        }
    }
}