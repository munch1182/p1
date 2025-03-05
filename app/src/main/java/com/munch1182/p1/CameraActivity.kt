package com.munch1182.p1

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.fragment.app.FragmentActivity
import com.munch1182.lib.PermissionHelper
import com.munch1182.lib.appDetailsPage
import com.munch1182.libcamera.Scan2Activity
import com.munch1182.p1.ui.theme.P1Theme

class CameraActivity : FragmentActivity() {

    private val scan =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    // 注册权限回调
    private val rfa = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (!it) {
            startActivity(appDetailsPage)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentWithBase { Camera() }
    }

    @Composable
    fun Camera() {
        val ctx = LocalContext.current
        Button({
            if (!PermissionHelper.check(android.Manifest.permission.CAMERA)) {
                rfa.launch(android.Manifest.permission.CAMERA)
                return@Button
            }
            scan.launch(Intent(ctx, Scan2Activity::class.java))
        }) { Text("开始扫描") }
    }

    @Preview
    @Composable
    fun CameraPreview() {
        P1Theme { Camera() }
    }
}

