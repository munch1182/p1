package com.munch1182.p1.views

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.munch1182.lib.base.appSetting
import com.munch1182.lib.base.blueSetting
import com.munch1182.lib.base.isBluetoothOpen
import com.munch1182.lib.base.isGpsOpen
import com.munch1182.lib.base.launchIO
import com.munch1182.lib.base.locSetting
import com.munch1182.lib.helper.result.ifAll
import com.munch1182.lib.helper.result.ifTrue
import com.munch1182.lib.helper.result.judge
import com.munch1182.lib.helper.result.manualIntent
import com.munch1182.lib.helper.result.permission
import com.munch1182.p1.base.BaseActivity
import com.munch1182.p1.base.onIntent
import com.munch1182.p1.base.onPermission
import com.munch1182.p1.ui.ClickButton
import com.munch1182.p1.ui.SpacerV
import com.munch1182.p1.ui.setContentWithRv

class ResultActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentWithRv { Views() }
    }

    @Composable
    private fun Views() {
        var result by remember { mutableStateOf("") }
        ClickButton("设置界面") { startActivity(appSetting()) }
        SpacerV()
        val callback: (Any) -> Unit = { result = it.toString() }
        ClickButton("相机权限") {
            callback("")
            permission(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                .onPermission("相机" to "拍摄视频", "麦克风" to "录音和音频通话")
                .manualIntent()
                .request(callback)
        }
        ClickButton("蓝牙权限") {
            callback("")
            val permission = mutableListOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                permission.addAll(arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT))
            }
            lifecycleScope.launchIO {
                judge({ isBluetoothOpen() }, blueSetting())
                    .onIntent("请前往蓝牙界面打开蓝牙，以使用蓝牙功能")
                    .ifTrue()
                    .permission(permission.toTypedArray())
                    .onPermission("蓝牙" to "蓝牙相关功能")
                    .manualIntent()
                    .ifAll()
                    .judge({ isGpsOpen() }, locSetting())
                    .onIntent("请前往位置界面打开定位，以扫描附近蓝牙设备")
                    .ifTrue()
                    .permission(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    .onPermission("定位" to "扫描附近蓝牙设备")
                    .manualIntent()
                    .ifAll()
                    .request(callback)
            }
        }

        Text(result)
    }
}