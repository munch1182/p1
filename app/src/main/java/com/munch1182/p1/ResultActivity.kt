package com.munch1182.p1

import android.Manifest.permission
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.fragment.app.FragmentActivity
import com.munch1182.lib.base.appDetailsPage
import com.munch1182.lib.base.findActivity
import com.munch1182.lib.result.ContractHelper
import com.munch1182.lib.result.IntentHelper
import com.munch1182.lib.result.PermissionHelper
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
            ContractHelper.init(ctx)
                .contract(ActivityResultContracts.PickVisualMedia())
                .input(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                .request { data = it.toString() }
        }
        ButtonDefault("从其他应用中粘贴") {
            IntentHelper.init(ctx)
                .judgeFirst { Settings.canDrawOverlays(it) }
                .intent(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
                .request { data = it.toString() }
        }
        ButtonDefault("跳转activity获取数据") {
            IntentHelper.init(ctx)
                .intent(Intent(ctx, WebActivity::class.java))
                .request { data = it.toString() }
        }
        Split()
        ButtonDefault("扫一扫") {
            PermissionHelper.init(ctx)
                .permission(arrayOf(permission.CAMERA))
                .request { data = it.toString() }
        }
        ButtonDefault("录制一段音频") {
            PermissionHelper.init(ctx)
                .permission(arrayOf(permission.RECORD_AUDIO, permission.ACCESS_FINE_LOCATION))
                .request { data = it.toString() }
        }
        ButtonDefault("连接蓝牙设备") {
            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(
                    permission.BLUETOOTH,
                    permission.BLUETOOTH_ADMIN,
                    permission.BLUETOOTH_SCAN,
                    permission.BLUETOOTH_CONNECT,
                    permission.BLUETOOTH_ADVERTISE,
                )
            } else {
                arrayOf(
                    permission.BLUETOOTH,
                    permission.BLUETOOTH_ADMIN,
                )
            }
            PermissionHelper.init(ctx)
                .permission(permission)
                .request { data = it.toString() }
        }

        Text(data)
    }

    @Preview
    @Composable
    fun ResultPreview() {
        P1Theme { Result() }
    }
}
