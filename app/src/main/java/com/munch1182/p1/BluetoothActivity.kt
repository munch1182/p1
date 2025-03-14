package com.munch1182.p1

import android.Manifest.permission
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.munch1182.lib.AppHelper
import com.munch1182.lib.helper.LocationHelper
import com.munch1182.lib.result.IntentDialogCreator
import com.munch1182.lib.result.PermissionDialogCreator
import com.munch1182.lib.result.PermissionHelper
import com.munch1182.lib.result.asResultDialog
import com.munch1182.lib.result.ifAllGrant
import com.munch1182.lib.result.permission
import com.munch1182.p1.ui.ButtonDefault

class BluetoothActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentWithBase { BluetoothView() }
    }

    @Composable
    fun BluetoothView() {
        var msg by remember { mutableStateOf("") }
        ButtonDefault("扫描") {
            permission {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    arrayOf(
                        permission.BLUETOOTH, permission.BLUETOOTH_ADMIN, permission.BLUETOOTH_CONNECT, permission.BLUETOOTH_SCAN, permission.BLUETOOTH_ADVERTISE
                    )
                } else {
                    arrayOf(permission.BLUETOOTH, permission.BLUETOOTH_ADMIN)
                }
            }.intentIfDeniedForever()
                .dialogWhen(newBluetoothIntentDialog())
                .ifAllGrant()
                // 必须分开请求
                .permission(permission.ACCESS_FINE_LOCATION, permission.ACCESS_COARSE_LOCATION)
                .dialogWhen(newBluetoothLocIntentDialog())
                .intentIfDeniedForever()
                .ifAllGrant()
                .judge { LocationHelper.isGpsOpen }
                .dialogBefore(newGpsDialog())
                .intent(LocationHelper.gpsOpenIntent)
                .request {
                    msg = "权限成功"
                }
        }
        Text(msg)
    }

    private fun newGpsDialog(): IntentDialogCreator {
        return { newDialog("蓝牙扫描需要GPS，请打开定位服务。", "去开启") }
    }

    private fun newBluetoothIntentDialog(): PermissionDialogCreator {
        return { _, state -> if (!state.isIntent) null else newDialog("蓝牙功能需要蓝牙权限，请前往设置页面打开蓝牙权限") }
    }

    private fun newBluetoothLocIntentDialog(): PermissionDialogCreator {
        return { _, state ->
            val str: Pair<String, String>? = when (state) {
                PermissionHelper.State.Denied -> "蓝牙属于附近设备，请授权定位权限以扫描蓝牙设备。" to "授权"
                PermissionHelper.State.Intent -> "蓝牙属于附近设备，没有定位权限无法扫描蓝牙设备，请前往设置页面打开定位权限。" to "前往"
                else -> null
            }
            str?.let { newDialog(it.first, it.second) }
        }
    }

    private fun Context.newDialog(str: String, okStr: String? = null, title: String? = null, needCancel: Boolean = true) =
        AlertDialog.Builder(this).setTitle(title ?: "请确认").setMessage(str).setPositiveButton(okStr ?: AppHelper.getString(android.R.string.ok)) { _, _ -> }.apply { if (needCancel) setNegativeButton(android.R.string.cancel) { _, _ -> } }.create().asResultDialog()

}