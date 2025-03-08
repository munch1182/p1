package com.munch1182.p1

import android.Manifest.permission
import android.bluetooth.BluetoothAdapter
import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.fragment.app.FragmentActivity
import com.munch1182.lib.AppHelper
import com.munch1182.lib.base.appDetailsPage
import com.munch1182.lib.base.findActivity
import com.munch1182.lib.base.getMediaPath
import com.munch1182.lib.base.shareTextIntent
import com.munch1182.lib.helper.LocationHelper
import com.munch1182.lib.helper.isBatteryOptimization
import com.munch1182.lib.helper.requestBatteryOptimizationIntent
import com.munch1182.lib.result.IntentDialogCreator
import com.munch1182.lib.result.IntentHelper
import com.munch1182.lib.result.PermissionDialogCreator
import com.munch1182.lib.result.asResultDialog
import com.munch1182.lib.result.contract
import com.munch1182.lib.result.ifAllGrant
import com.munch1182.lib.result.ifOk
import com.munch1182.lib.result.intent
import com.munch1182.lib.result.judge
import com.munch1182.lib.result.permission
import com.munch1182.p1.ui.ButtonDefault
import com.munch1182.p1.ui.Split
import com.munch1182.p1.ui.theme.P1Theme

class ResultActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentWithBase { Result() }
    }

    @Composable
    fun Result() {
        var data by remember { mutableStateOf("") }
        val ctx = LocalContext.current.findActivity() as? FragmentActivity ?: return
        ButtonDefault("跳转app页面") { ctx.startActivity(appDetailsPage) }
        Split()
        ButtonDefault("从相册中选择") {
            contract(ActivityResultContracts.PickVisualMedia())
                .input(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                .mapResult { it?.getMediaPath() ?: "" }
                .request { data = it }
        }
        ButtonDefault("跳转activity获取数据") {
            intent(Intent(ctx, WebActivity::class.java))
                .request { data = it.toString() }
        }
        ButtonDefault("分享文本") {
            IntentHelper.init(ctx)
                .dialogBefore(newShareDialog())
                .intent(shareTextIntent("分享文本内容__来自${AppHelper.getString(R.string.app_name)}"))
                .request { data = it.toString() }
        }
        ButtonDefault("判断省电策略并跳转") {
            judge { isBatteryOptimization() }
                .intent(requestBatteryOptimizationIntent())
                .request { data = it.toString() }
        }
        ButtonDefault("从其他应用中粘贴文本") {
            judge { Settings.canDrawOverlays(it) }
                .dialogBefore(newOverlayDialog())
                .intent(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
                .request { data = it.toString() }
        }
        Split()
        ButtonDefault("扫一扫") {
            permission(permission.CAMERA)
                .dialogWhen(newScanDialog())
                .request { data = it.toString() }
        }
        ButtonDefault("录制一段音频") {
            permission(permission.RECORD_AUDIO)
                .dialogWhen(newRecordDialog())
                .request { data = it.toString() }
        }
        ButtonDefault("连接蓝牙设备") {
            permission {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    arrayOf(permission.BLUETOOTH, permission.BLUETOOTH_ADMIN, permission.BLUETOOTH_CONNECT, permission.BLUETOOTH_SCAN)
                } else {
                    arrayOf(permission.BLUETOOTH, permission.BLUETOOTH_ADMIN)
                }
            }.ifAllGrant()
                .judge { LocationHelper.isGpsOpen }
                .intent(LocationHelper.gpsOpenIntent)
                .ifOk()
                .judge { BluetoothAdapter.getDefaultAdapter()?.isEnabled == true }
                .intent(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                .request { data = it.toString() }
        }

        Text(data)
    }

    private fun newScanDialog(): PermissionDialogCreator = { _, state -> if (!state.isDenied) null else newDialog("扫一扫功能需要相机权限，请授权相机权限") }

    private fun newRecordDialog(): PermissionDialogCreator = { map, state ->
        val str = StringBuilder()
        if (state.isDenied && map.contains(permission.RECORD_AUDIO)) str.append("录音功能需要录音权限，请授权录音权限;").append("\r\n")
        if (str.isBlank()) null else newDialog(str.toString())
    }

    private fun newShareDialog(): IntentDialogCreator = { newDialog("即将分享，请注意保护个人信息。", "分享") }
    private fun newOverlayDialog(): IntentDialogCreator = { newDialog("从其他应用中粘贴文本需要悬浮窗权限，请在接下来的页面中选择本应用，并选择允许", "去授权") }
    private fun Context.newDialog(str: String, okStr: String? = null, title: String? = null, needCancel: Boolean = true) =
        AlertDialog.Builder(this).setTitle(title ?: "请确认").setMessage(str).setPositiveButton(okStr ?: AppHelper.getString(android.R.string.ok)) { _, _ -> }.apply { if (needCancel) setNegativeButton(android.R.string.cancel) { _, _ -> } }.create().asResultDialog()

    private fun newBlueLocDialog(): PermissionDialogCreator = { _, state ->
        if (state.isBefore) null else newDialog("蓝牙设备属于附近设备，所以需要定位权限，无定位权限则无法扫描到蓝牙，请授权定位权限")
    }

    @Preview
    @Composable
    fun ResultPreview() {
        P1Theme { Result() }
    }
}
