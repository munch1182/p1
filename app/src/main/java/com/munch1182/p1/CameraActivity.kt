package com.munch1182.p1

import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.tooling.preview.Preview
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.munch1182.lib.base.appDetailsPage
import com.munch1182.lib.base.asPermissionCheck
import com.munch1182.libcamera2.CameraHelper
import com.munch1182.libcamera2.registerScan
import com.munch1182.p1.ui.theme.P1Theme

class VM : ViewModel() {
    private var _result = MutableLiveData("")
    val result: LiveData<String> = _result

    fun updateResult(result: String) {
        _result.value = result
    }
}

class CameraActivity : FragmentActivity() {

    private val vm: VM by viewModels()


    private val scan = registerScan {
        vm.updateResult(it.contents ?: "")
    }

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
        val str by vm.result.observeAsState()
        Button({
            if (!android.Manifest.permission.CAMERA.asPermissionCheck()) {
                rfa.launch(android.Manifest.permission.CAMERA)
                return@Button
            }
            scan.launch(CameraHelper.intent())
        }) { Text("开始扫描二维码") }
        Text(str ?: "")
    }

    @Preview
    @Composable
    fun CameraPreview() {
        P1Theme { Camera() }
    }
}

