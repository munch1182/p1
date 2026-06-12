package com.munch1182.p1.screen

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.munch1182.core.base.AppAnalytics
import com.munch1182.core.ui.PrimaryButton
import com.munch1182.core.ui.ScrollPage
import com.munch1182.core.ui.currAsFragmentActivityOrThrow
import com.munch1182.core.ui.dialog.DialogFactory
import com.munch1182.core.ui.dialog.Notice
import com.munch1182.core.ui.permission.checkBluetoothPermission
import com.munch1182.core.ui.permission.checkNotificationPermission
import com.munch1182.lib.android.appSetting
import com.munch1182.lib.android.awaitResult
import com.munch1182.lib.android.createNotification
import com.munch1182.lib.android.createNotificationChannel
import com.munch1182.lib.android.notify
import com.munch1182.lib.android.result.requestResult
import com.munch1182.p1.R
import com.munch1182.p1.AppGraph
import com.ramcosta.composedestinations.annotation.Destination
import kotlinx.coroutines.launch

/**
 * 测试dialog或者权限/结果相关ui
 */
@Destination<AppGraph>
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
            scope.launch {
                val chose = DialogFactory.newYesNoDialog("选择是或者否", ok = "是", cancel = "否").awaitResult()
                str = "选择了$chose"
            }
        }
        PrimaryButton("蓝牙权限") {
            reset()
            scope.launch {
                checkBluetoothPermission(analytics = AppAnalytics) { permission -> str = "蓝牙权限: $permission" }
            }
        }
        PrimaryButton("发出通知") {
            reset()
            scope.launch {
                checkNotificationPermission {
                    val channelId = "testChannel"
                    createNotificationChannel(channelId, "test", "这是一个测试通知")
                    notify(
                        createNotification(
                            "通知", "这是一个通知",
                            R.drawable.ic_launcher_foreground,
                            channelId
                        ), 1
                    )
                }
            }
        }
        PrimaryButton("应用设置") {
            reset()
            scope.launch {
                currAsFragmentActivityOrThrow.requestResult(appSetting)
            }
        }
        Text(str)
    }
}