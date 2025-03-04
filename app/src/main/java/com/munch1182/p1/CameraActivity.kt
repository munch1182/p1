package com.munch1182.p1

import android.Manifest
import android.os.Bundle
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.fragment.app.FragmentActivity
import com.munch1182.lib.findActivity
import com.munch1182.lib.result.with
import com.munch1182.p1.ui.theme.P1Theme

class CameraActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentWithBase { Camera() }
    }
}

@Composable
fun Camera() {
    val act = LocalContext.current.findActivity() as? FragmentActivity ?: return
    Button({
        act.with(Manifest.permission.CAMERA)
            .request {

            }
    }) { Text("权限申请") }
}

@Preview
@Composable
fun CameraPreview() {
    P1Theme { Camera() }
}