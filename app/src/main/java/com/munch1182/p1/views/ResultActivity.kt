package com.munch1182.p1.views

import android.Manifest
import android.content.Intent
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
import com.munch1182.lib.helper.result.AllowDenyDialogContainer
import com.munch1182.lib.helper.result.ContractHelper
import com.munch1182.lib.helper.result.IntentHelper
import com.munch1182.lib.helper.result.JudgeHelper
import com.munch1182.lib.helper.result.PermissionHelper
import com.munch1182.lib.helper.result.PermissionHelper.PermissionCanRequestDialogProvider
import com.munch1182.lib.helper.result.asAllowDenyDialog
import com.munch1182.p1.base.BaseActivity
import com.munch1182.p1.base.curr
import com.munch1182.p1.base.currFM
import com.munch1182.p1.ui.ClickButton
import com.munch1182.p1.ui.JumpButton
import com.munch1182.p1.ui.Split
import com.munch1182.p1.ui.setContentWithBase

class ResultActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentWithBase { View() }
    }

    @Composable
    private fun View() {
        var result by remember { mutableStateOf("") }
        val callback: (Any?) -> Unit = { result = it.toString() }

        JumpButton("app设置", intent = appSetting())
        Split()
        ClickButton("选择图片") {
            ContractHelper.init(currFM)
                .contract(ActivityResultContracts.PickVisualMedia())
                .input(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                .request(callback)
        }
        ClickButton("请求相机权限") {
            PermissionHelper.init(currFM)
                .permission(arrayOf(Manifest.permission.CAMERA))
                .dialogWhen(permissionDialog("相机"))
                .manualIntent()
                .request(callback)
        }
        ClickButton("请求跳转") {
            IntentHelper.init(currFM)
                .intent(WebContentActivity.url(curr))
                .request(callback)
        }
        ClickButton("悬浮窗权限") {
            JudgeHelper.init(currFM)
                .judge { Settings.canDrawOverlays(it) }
                .intent(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).wPName())
                .dialogWhen { _, state ->
                    intentDialog(
                        when (state) {
                            JudgeHelper.State.Before -> "请在接下来的页面中选择本应用，并选择允许，以便使用跨应用相关功能。"
                            JudgeHelper.State.After -> "请在接下来的页面中选择本应用，并选择允许，否则无法使用跨应用相关功能。"
                        }
                    )
                }
                .request(callback)
        }

        Text(result)
    }

    private fun intentDialog(msg: String, title: String = "跳转", sure: String = "前往"): AllowDenyDialogContainer {
        return AlertDialog.Builder(curr).setTitle(title)
            .setMessage(msg)
            .setPositiveButton(sure) { _, _ -> }
            .setNegativeButton("拒绝") { _, _ -> }
            .create().asAllowDenyDialog()
    }

    private fun permissionDialog(name: String): PermissionCanRequestDialogProvider {
        return PermissionCanRequestDialogProvider { ctx, state, p ->
            val (msg, ok) = when (state) {
                PermissionHelper.State.Before -> return@PermissionCanRequestDialogProvider null
                PermissionHelper.State.Denied -> "正在申请${name}相关权限, 用于拍摄照片。" to "授权"
                PermissionHelper.State.DeniedForever -> "需要前往设置页面手动开启${name}权限, 否则该功能无法使用。" to "去开启"
            }
            AlertDialog.Builder(ctx).setTitle("权限请求").setMessage(msg)
                .setPositiveButton(ok) { _, _ -> }
                .setNegativeButton("拒绝") { _, _ -> }
                .create().asAllowDenyDialog()
        }
    }
}