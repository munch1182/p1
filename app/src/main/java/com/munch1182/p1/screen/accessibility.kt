package com.munch1182.p1.screen

import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.munch1182.core.android.awaitResult
import com.munch1182.core.android.isAccessibilityEnabled
import com.munch1182.core.android.result.requestResult
import com.munch1182.core.common.launchMain
import com.munch1182.p1.base.AppAccessibilityService
import com.munch1182.p1.base.currAsFragmentActivityOrThrow
import com.munch1182.p1.dialog.Dialog
import com.munch1182.p1.ui.PrimaryButton
import com.munch1182.p1.ui.RunningStateButton
import com.munch1182.p1.ui.ScrollPage
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph

@Destination<RootGraph>
@Composable
fun AccessibilityScreen() {
    val check = { isAccessibilityEnabled(AppAccessibilityService::class.java) }
    var isEnable by remember { mutableStateOf(check()) }
    val scope = rememberCoroutineScope()
    ScrollPage {
        RunningStateButton(isEnable, text = if (isEnable) "使用中" else "启用无障碍服务") {
            if (isEnable) {
                scope.launchMain {
                    enableAccessibilityServer()
                    isEnable = check()
                }
            }
        }
        PrimaryButton("去开启无线调试") { }
        PrimaryButton("去关闭无线调试") { }
    }
}

private suspend fun enableAccessibilityServer() {
    val result = Dialog.newYesNoDialog("请前往无障碍服务器进行授权").awaitResult()
    if (!result) return
    currAsFragmentActivityOrThrow.requestResult(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
}