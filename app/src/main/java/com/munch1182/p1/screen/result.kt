package com.munch1182.p1.screen

import android.content.Intent
import android.provider.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.munch1182.core.android.appSetting
import com.munch1182.core.android.awaitResult
import com.munch1182.core.android.developerOptionsIntent
import com.munch1182.core.android.isDeveloperOpen
import com.munch1182.core.android.newTask
import com.munch1182.core.android.result.requestResult
import com.munch1182.core.common.launchMain
import com.munch1182.p1.base.checkBluetoothPermission
import com.munch1182.p1.base.currAsFragmentActivityOrThrow
import com.munch1182.p1.dialog.Dialog
import com.munch1182.p1.dialog.Notice
import com.munch1182.p1.ui.PrimaryButton
import com.munch1182.p1.ui.ScrollPage
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph

@Destination<RootGraph>
@Composable
fun ResultScreen() {
    val scope = rememberCoroutineScope()
    var str by remember { mutableStateOf("") }
    val reset = { str = "" }
    ScrollPage {
        PrimaryButton("提示") {
            reset()
            Notice.toast("提示, 使用后台context")
        }
        PrimaryButton("提示弹窗") {
            reset()
            scope.launchMain {
                val chose = Dialog.newYesNoDialog("选择是或者否", ok = "是", cancel = "否").awaitResult()
                str = "选择了$chose"
            }
        }
        PrimaryButton("蓝牙权限") {
            reset()
            scope.launchMain {
                checkBluetoothPermission { permission -> str = "蓝牙权限: $permission" }
            }
        }
        PrimaryButton("应用设置") {
            reset()
            scope.launchMain {
                currAsFragmentActivityOrThrow.requestResult(appSetting)
            }
        }
        PrimaryButton("开发者选项") {
            reset()
            scope.launchMain { openDeveloperOptions() }
        }
        Text(str)
    }
}

private suspend fun openDeveloperOptions() {
    val curr = currAsFragmentActivityOrThrow
    if (!isDeveloperOpen) {
        // 开发者选项未打开
        val isGoto = Dialog.newYesNoDialog("开发者选项未打开, 是否前往打开", ok = "前往").awaitResult()
        if (isGoto) {
            curr.requestResult(Intent(Settings.ACTION_DEVICE_INFO_SETTINGS).newTask)
            return openDeveloperOptions()
        }
    } else {
        curr.requestResult(developerOptionsIntent.newTask)
    }
}