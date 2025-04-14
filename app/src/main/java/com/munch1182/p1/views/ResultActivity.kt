package com.munch1182.p1.views

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.munch1182.lib.base.appSetting
import com.munch1182.lib.base.wPName
import com.munch1182.lib.helper.blue.BluetoothHelper
import com.munch1182.lib.helper.currAct
import com.munch1182.lib.helper.isLocationProvider
import com.munch1182.lib.helper.result.AllowDenyDialogContainer
import com.munch1182.lib.helper.result.JudgeHelper
import com.munch1182.lib.helper.result.JudgeHelper.IntentCanLaunchDialogProvider
import com.munch1182.lib.helper.result.PermissionHelper
import com.munch1182.lib.helper.result.PermissionHelper.PermissionCanRequestDialogProvider
import com.munch1182.lib.helper.result.asAllowDenyDialog
import com.munch1182.lib.helper.result.contract
import com.munch1182.lib.helper.result.ifAllGranted
import com.munch1182.lib.helper.result.ifTrue
import com.munch1182.lib.helper.result.intent
import com.munch1182.lib.helper.result.judge
import com.munch1182.lib.helper.result.permission
import com.munch1182.lib.helper.result.permissions
import com.munch1182.p1.base.BaseActivity
import com.munch1182.p1.ui.ClickButton
import com.munch1182.p1.ui.JumpButton
import com.munch1182.p1.ui.Split
import com.munch1182.p1.ui.setContentWithRv

class ResultActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentWithRv { View() }
    }

    @Composable
    private fun View() {
        var result by remember { mutableStateOf("") }
        val callback: (Any?) -> Unit = { result = it.toString() }

        JumpButton("app设置", intent = appSetting())
        Split()
        ClickButton("选择图片") {
            contract(ActivityResultContracts.PickVisualMedia())
                .input(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                .request(callback)
        }
        ClickButton("请求相机权限") {
            permission(Manifest.permission.CAMERA)
                .dialogWhen(permissionDialog("相机", "拍摄照片"))
                .manualIntent()
                .request(callback)
        }
        ClickButton("请求跳转") {
            intent(WebContentActivity.url(currAct)).request(callback)
        }
        ClickButton("悬浮窗权限") {
            judge { Settings.canDrawOverlays(it) }
                .intent(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).wPName())
                .dialogWhen(intentDialog("请在接下来的页面中选择本应用，并选择允许，以便使用跨应用相关功能。", "请在接下来的页面中选择本应用，并选择允许，否则无法使用跨应用相关功能。"))
                .request(callback)
        }

        Split()

        ClickButton("蓝牙权限") {
            val p = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                arrayOf(Manifest.permission.BLUETOOTH)
            }
            permissions(p)
                .dialogWhen(permissionDialog("蓝牙", "扫描蓝牙"))
                .manualIntent()
                .ifAllGranted()
                .judge { BluetoothHelper.isBlueOn }
                .intent(BluetoothHelper.enableBlueIntent())
                .ifTrue()
                .judge { isLocationProvider }
                .intent(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                .dialogWhen(intentBlueScan())
                .ifTrue()
                .permission(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION) // 定位打开此权限才会判断为true
                .dialogWhen(permissionBlueScan())
                .ifAllGranted()
                .requestAll(callback)

        }

        Text(result)
    }

    private fun intentDialog(before: String?, after: String?): IntentCanLaunchDialogProvider {
        return IntentCanLaunchDialogProvider { _, state ->
            val msg = when (state) {
                JudgeHelper.State.Before -> before
                JudgeHelper.State.After -> after
            } ?: return@IntentCanLaunchDialogProvider null
            allowDenyDialog(msg, "前往授权", "前往")
        }
    }

    private fun intentBlueScan(): IntentCanLaunchDialogProvider {
        return IntentCanLaunchDialogProvider { _, _ -> allowDenyDialog("蓝牙扫描属于附近设备，所以需要定位功能及权限，请打开定位功能", "权限申请", "去打开") }
    }

    private fun permissionBlueScan(): PermissionCanRequestDialogProvider {
        return PermissionCanRequestDialogProvider { _, state, _ ->
            val (msg, ok) = when (state) {
                PermissionHelper.State.DeniedForever -> "定位权限已被拒绝, 需要前往设置页面手动开启定位权限, 否则无法扫描到蓝牙设备。" to "去开启"
                else -> "蓝牙扫描附近设备，需要定位权限，请允许定位权限" to "允许"
            }
            allowDenyDialog(msg, "蓝牙扫描权限", ok)
        }
    }

    private fun permissionDialog(name: String, toUse: String, noBefore: Boolean = true, noDenied: Boolean = false): PermissionCanRequestDialogProvider {
        return PermissionCanRequestDialogProvider { _, state, _ ->
            val (msg, ok) = when (state) {
                PermissionHelper.State.Before -> if (noBefore) return@PermissionCanRequestDialogProvider null else "正在申请${name}相关权限, 用于${toUse}。" to "允许"
                PermissionHelper.State.Denied -> if (noDenied) return@PermissionCanRequestDialogProvider null else "正在申请${name}相关权限, 用于${toUse}。" to "允许"
                PermissionHelper.State.DeniedForever -> "该权限已被拒绝, 需要前往设置页面手动开启${name}权限, 否则该功能无法使用。" to "去开启"
            }
            allowDenyDialog(msg, "权限申请", ok)
        }
    }

    private fun allowDenyDialog(msg: String, title: String, sure: String = "确定"): AllowDenyDialogContainer {
        return AlertDialog.Builder(currAct).setTitle(title)
            .setMessage(msg)
            .setPositiveButton(sure) { _, _ -> }
            .setNegativeButton("拒绝") { _, _ -> }
            .create().asAllowDenyDialog()
    }
}