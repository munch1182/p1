package com.munch1182.p1

import android.Manifest.permission
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
import com.munch1182.lib.result.PermissionHelper
import com.munch1182.lib.result.asResultDialog
import com.munch1182.lib.result.contract
import com.munch1182.lib.result.intent
import com.munch1182.lib.result.judgeFirst
import com.munch1182.lib.result.needDeniedDialog
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
                .request { data = it.toString() }
        }
        ButtonDefault("从其他应用中粘贴文本") {
            judgeFirst { Settings.canDrawOverlays(it) }
                .dialogIfNeed {
                    it.newDialog("从其他应用中粘贴文本需要开启悬浮窗，请在跳转的页面列表中选择本应用，并随后选择允许。", "去授权", "权限请求")
                }
                .intent(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
                .request { data = it.toString() }
        }
        ButtonDefault("跳转activity获取数据") {
            intent(WebActivity::class.java).request { data = it.toString() }
        }
        Split()
        ButtonDefault("扫一扫") {
            permission(permission.CAMERA).request { data = it.toString() }
        }
        ButtonDefault("录制一段音频") {
            PermissionHelper.init(ctx)
                .dialogIfNeed { context, state, r ->
                    if (!state.isBefore && r.needDeniedDialog(permission.RECORD_AUDIO)) {
                        context.newDialog("没有该权限则录制功能不可用，请授予录音权限！", "去授权", "录音权限")
                    } else {
                        null
                    }

                }
                .permission(arrayOf(permission.RECORD_AUDIO))
                .request { data = it.toString() }
        }
        ButtonDefault("连接蓝牙设备") {
            permission {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
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
            }.request { data = it.toString() }
        }

        Text(data)
    }

    private fun Context.newDialog(str: String, okStr: String? = null, title: String? = null, needCancel: Boolean = true) =
        AlertDialog.Builder(this)
            .setTitle(title ?: "请确认")
            .setMessage(str)
            .setPositiveButton(okStr ?: AppHelper.getString(android.R.string.ok)) { _, _ -> }
            .apply { if (needCancel) setNegativeButton(android.R.string.cancel) { _, _ -> } }
            .create()
            .asResultDialog()

    @Preview
    @Composable
    fun ResultPreview() {
        P1Theme { Result() }
    }
}
