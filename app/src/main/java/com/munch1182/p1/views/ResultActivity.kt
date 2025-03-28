package com.munch1182.p1.views

import android.Manifest
import android.content.Intent
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
import com.munch1182.lib.base.wPName
import com.munch1182.lib.helper.result.ContractHelper
import com.munch1182.lib.helper.result.IntentHelper
import com.munch1182.lib.helper.result.JudgeHelper
import com.munch1182.lib.helper.result.PermissionHelper
import com.munch1182.p1.base.BaseActivity
import com.munch1182.p1.base.curr
import com.munch1182.p1.base.currFM
import com.munch1182.p1.ui.ClickButton
import com.munch1182.p1.ui.setContentWithBase

class ResultActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentWithBase { View() }
    }

    @Composable
    fun View() {
        var result by remember { mutableStateOf("") }
        ClickButton("选择图片") {
            ContractHelper.init(currFM).contract(ActivityResultContracts.PickVisualMedia()).input(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)).request { result = it.toString() }
        }
        ClickButton("请求相机权限") {
            PermissionHelper.init(currFM).permission(arrayOf(Manifest.permission.CAMERA)).request { result = it.toString() }
        }
        ClickButton("请求跳转") {
            IntentHelper.init(currFM).intent(WebContentActivity.url(curr, "https://www.baidu.com")).request { result = it.toString() }
        }
        ClickButton("悬浮窗权限") {
            JudgeHelper.init(currFM).judge { Settings.canDrawOverlays(it) }.intent(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).wPName()).request { result = it.toString() }
        }

        Text(result)
    }
}