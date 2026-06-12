package com.munch1182.p1.screen

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.munch1182.core.ui.RunningStateButton
import com.munch1182.core.ui.ScrollPage
import com.munch1182.core.ui.currAsFragmentActivityOrThrow
import com.munch1182.core.ui.dialog.DialogFactory
import com.munch1182.lib.android.AppHelper
import com.munch1182.lib.android.accessibility.AccessibilityServiceHelper
import com.munch1182.lib.android.accessibility.className
import com.munch1182.lib.android.accessibility.findByScrollOrNull
import com.munch1182.lib.android.accessibility.isAccessibilityEnabled
import com.munch1182.lib.android.accessibility.plus
import com.munch1182.lib.android.accessibility.startClickIfCan
import com.munch1182.lib.android.accessibility.windowType
import com.munch1182.lib.android.awaitResult
import com.munch1182.lib.android.developerOptionsIntent
import com.munch1182.lib.android.isDeveloperOpen
import com.munch1182.lib.android.logFailure
import com.munch1182.lib.android.newTask
import com.munch1182.lib.android.result.requestResult
import com.munch1182.lib.common.launchMain
import com.munch1182.p1.base.AppAccessibilityService
import com.munch1182.p1.AppGraph
import com.ramcosta.composedestinations.annotation.Destination
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 无障碍相关设置ui
 */
@Destination<AppGraph>
@Composable
fun AccessibilityScreen() {
    val check = { isAccessibilityEnabled(AppAccessibilityService::class.java) }
    var isEnable by remember { mutableStateOf(check()) }
    val scope = rememberCoroutineScope()
    ScrollPage {
        RunningStateButton(isEnable, text = if (isEnable) "无障碍服务已授权" else "启用无障碍服务") {
            if (!isEnable) {
                scope.launchMain {
                    enableAccessibilityServer()
                    isEnable = check()
                }
            }
        }
        if (isEnable) AdbWifiSetting()

    }
}

private suspend fun startAdbWifiTask(start: Boolean) {
    val server = AccessibilityServiceHelper.server ?: return
    AccessibilityServiceHelper.awaitEvent(
        match = windowType() + className($$"com.android.settings.Settings$DevelopmentSettingsDashboardActivity")
    ) ?: return
    val node = server.rootInActiveWindow?.findByScrollOrNull("无线调试") ?: return
    delay(1000) // 监听idle不如直接delay有效
    node.startClickIfCan()
}

@Composable
private fun AdbWifiSetting() {
    val isEnable by remember { mutableStateOf(isWirelessAdbEnabled()) }
    val scope = rememberCoroutineScope()
    RunningStateButton(isEnable, text = if (isEnable) "去关闭无线调试" else "去启用无线调试") {
        scope.launch {
            openDeveloperOptions()
            if (isDeveloperOpen) scope.launch { startAdbWifiTask(!isEnable) }
        }
    }
}


private suspend fun openDeveloperOptions() {
    val curr = currAsFragmentActivityOrThrow
    if (!isDeveloperOpen) {
        // 开发者选项未打开
        val isGoto = DialogFactory.newYesNoDialog("开发者选项未打开, 是否前往打开", ok = "前往").awaitResult() ?: false
        if (isGoto) {
            curr.requestResult(Intent(Settings.ACTION_DEVICE_INFO_SETTINGS).newTask)
            return openDeveloperOptions()
        }
    } else {
        curr.requestResult(developerOptionsIntent.newTask)
    }
}

/**
 * 检查设备的无线调试（Wireless ADB）是否已开启。
 * @return true 表示无线调试已开启，false 表示未开启。
 */
private fun isWirelessAdbEnabled(context: Context = AppHelper): Boolean {
    return runCatching {
        // 从系统全局设置中读取 "adb_wifi_enabled" 的值
        Settings.Global.getInt(context.contentResolver, "adb_wifi_enabled", 0) != 0
    }.logFailure("从系统全局设置中读取adb_wifi_enabled").getOrNull() ?: false
}

private suspend fun enableAccessibilityServer() {
    val result = DialogFactory.newYesNoDialog("请前往无障碍服务器进行授权").awaitResult() ?: false
    if (!result) return
    currAsFragmentActivityOrThrow.requestResult(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
}