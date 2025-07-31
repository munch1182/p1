package com.munch1182.p1.views

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.munch1182.lib.base.appSetting
import com.munch1182.lib.base.isInDeveloperMode
import com.munch1182.lib.helper.blue.BluetoothHelper
import com.munch1182.lib.helper.isLocationProvider
import com.munch1182.lib.helper.result.JudgeHelper.IntentCanLaunchDialogProvider
import com.munch1182.lib.helper.result.ifAllGranted
import com.munch1182.lib.helper.result.ifTrue
import com.munch1182.lib.helper.result.intent
import com.munch1182.lib.helper.result.judge
import com.munch1182.lib.helper.result.onTrue
import com.munch1182.lib.helper.result.permission
import com.munch1182.lib.helper.result.permissions
import com.munch1182.p1.base.BaseActivity
import com.munch1182.p1.base.DialogHelper
import com.munch1182.p1.base.dialogPermission
import com.munch1182.p1.base.intentBlueScanDialog
import com.munch1182.p1.base.permissionBlueScanDialog
import com.munch1182.p1.ui.ClickButton
import com.munch1182.p1.ui.JumpButton
import com.munch1182.p1.ui.SplitV
import com.munch1182.p1.ui.setContentWithRv

class ResultActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentWithRv { Views() }
    }

    @Composable
    private fun Views() {
        var result by remember { mutableStateOf("") }
        val callback: (Any?) -> Unit = { result = it.toString() }

        JumpButton("应用详情", intent = appSetting())

        SplitV()

        ClickButton("相机") {
            permission(Manifest.permission.CAMERA).dialogPermission("相机", "拍摄").manualIntent().request(callback)
        }
        ClickButton("开发者选项") {
            val devIntent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            judge { isInDeveloperMode() }.intent(Intent(Settings.ACTION_DEVICE_INFO_SETTINGS)).dialogWhen(developerDialog()).onTrue { intent(devIntent).request {} }
        }

        SplitV()

        ClickButton("蓝牙") {
            val p = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                arrayOf(Manifest.permission.BLUETOOTH)
            }
            permissions(p)
                .dialogPermission("蓝牙", "扫描蓝牙")
                .manualIntent()
                .ifAllGranted()
                .judge { BluetoothHelper.isBlueOn }
                .intent(BluetoothHelper.enableBlueIntent())
                .ifTrue()
                .judge { isLocationProvider }
                .intent(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                .intentBlueScanDialog()
                .ifTrue()
                .permission(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION) // 定位打开此权限才会判断为true
                .permissionBlueScanDialog().ifAllGranted().requestAll(callback)
        }

        SplitV()
        Text(result)
    }

    private fun developerDialog(): IntentCanLaunchDialogProvider {
        return IntentCanLaunchDialogProvider { _, state ->
            if (state.isAfter) {
                null
            } else {
                DialogHelper.newMessage("请前往开发者选项开启开发者模式", "开发者模式未打开", "前往")
            }
        }
    }
}