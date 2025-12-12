package com.munch1182.p1.views

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.munch1182.android.lib.base.isBluetoothOpen
import com.munch1182.android.lib.base.isDeveloperMode
import com.munch1182.android.lib.base.appSetting
import com.munch1182.android.lib.helper.result.contract
import com.munch1182.android.lib.helper.result.ifAllGranted
import com.munch1182.android.lib.helper.result.ifTrue
import com.munch1182.android.lib.helper.result.ignoreIf
import com.munch1182.android.lib.helper.result.intent
import com.munch1182.android.lib.helper.result.judge
import com.munch1182.android.lib.helper.result.permission
import com.munch1182.p1.base.appIntent
import com.munch1182.p1.base.onDialog
import com.munch1182.p1.ui.ClickButton
import com.munch1182.p1.ui.Items
import com.munch1182.p1.ui.SpacerV

@Composable
fun ResultView() {
    var result by remember { mutableStateOf("") }
    val r: (Any?) -> Unit = { result = it.toString() }
    Items(Modifier.fillMaxWidth()) {
        ClickButton("选择") {
            contract(ActivityResultContracts.PickVisualMedia(), PickVisualMediaRequest())
                .onDialog("请选择一张图片，用作头像。")
                .request(r)
        }
        ClickButton("设置") {
            intent(appSetting()).request(r)
        }
        SpacerV()
        ClickButton("相机") {
            permission(listOf(Manifest.permission.CAMERA))
                .onDialog("相机", "拍摄")
                .appIntent()
                .request(r)
        }
        ClickButton("蓝牙") {
            val permission = mutableListOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                permission.addAll(arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN))
            }
            permission(permission)
                .onDialog("蓝牙", "查找、连接附近蓝牙设备")
                .appIntent()
                .ifAllGranted()
                .judge({ isBluetoothOpen() }, Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                .onDialog("打开蓝牙", "请打开蓝牙开关以使用蓝牙", ok = "去打开")
                .ignoreIf()
                .request(r)
        }
        SpacerV()
        ClickButton("开发者选项") {
            judge({ isDeveloperMode() }, Intent(Settings.ACTION_SETTINGS))
                .onDialog("打开开发者模式", "前往设置界面，反复点击Os版本直至提示已处于开发者模式", ok = "前往")
                .ifTrue()
                .intent(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
                .ignoreIf()
                .request(r)

        }
        SpacerV()
        Text(result)
    }
}