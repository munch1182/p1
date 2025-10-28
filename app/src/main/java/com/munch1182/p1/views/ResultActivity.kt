package com.munch1182.p1.views

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.lifecycle.lifecycleScope
import com.munch1182.lib.AppHelper
import com.munch1182.lib.base.appSetting
import com.munch1182.lib.base.blueSetting
import com.munch1182.lib.base.getPhoneNumbers
import com.munch1182.lib.base.isBluetoothOpen
import com.munch1182.lib.base.isGpsOpen
import com.munch1182.lib.base.launchIO
import com.munch1182.lib.base.locSetting
import com.munch1182.lib.base.toDateStr
import com.munch1182.lib.helper.FileHelper
import com.munch1182.lib.helper.NoticeHelper
import com.munch1182.lib.helper.result.ifAll
import com.munch1182.lib.helper.result.ifTrue
import com.munch1182.lib.helper.result.isAllGranted
import com.munch1182.lib.helper.result.judge
import com.munch1182.lib.helper.result.manualIntent
import com.munch1182.lib.helper.result.permission
import com.munch1182.lib.helper.toProvider
import com.munch1182.p1.base.BaseActivity
import com.munch1182.p1.base.onIntent
import com.munch1182.p1.base.onPermission
import com.munch1182.p1.ui.ClickButton
import com.munch1182.p1.ui.SpacerV
import com.munch1182.p1.ui.setContentWithRv
import kotlinx.coroutines.delay

class ResultActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentWithRv { Views() }
    }

    @SuppressLint("MissingPermission")
    @Composable
    private fun Views() {
        var result by remember { mutableStateOf("") }
        ClickButton("设置界面") { startActivity(appSetting()) }
        SpacerV()
        val callback: (Any) -> Unit = { result = it.toString() }
        ClickButton("拍照") {
            FileHelper.newCache("img", "img.png").toProvider()?.let { uri ->
                permission(Manifest.permission.CAMERA)
                    .onPermission("相机" to "拍照")
                    .manualIntent()
                    .ifAll()
                    .contract(ActivityResultContracts.TakePicture(), uri)
                    .ifAny { it == true }
                    .request(callback)
            }
        }
        ClickButton("相机权限") {
            callback("")
            permission(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                .onPermission("相机" to "拍摄视频", "麦克风" to "录音和音频通话")
                .manualIntent()
                .request(callback)
        }
        ClickButton("蓝牙权限") {
            callback("")
            val permission = mutableListOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                permission.addAll(arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT))
            }
            lifecycleScope.launchIO {
                judge({ isBluetoothOpen() }, blueSetting())
                    .onIntent("请前往蓝牙界面打开蓝牙，以使用蓝牙功能")
                    .ifTrue()
                    .permission(permission.toTypedArray())
                    .onPermission("蓝牙" to "蓝牙相关功能")
                    .manualIntent()
                    .ifAll()
                    .judge({ isGpsOpen() }, locSetting())
                    .onIntent("请前往位置界面打开定位，以扫描附近蓝牙设备")
                    .ifTrue()
                    .permission(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    .onPermission("定位" to "扫描附近蓝牙设备")
                    .manualIntent()
                    .ifAll()
                    .request(callback)
            }
        }
        SpacerV()

        ClickButton("发送通知") {
            sendNotice(content = "现在是${System.currentTimeMillis().toDateStr()}")
        }
        ClickButton("发送进度") {
            sendNotice(content = "0%") { id ->
                lifecycleScope.launchIO {
                    var i = 0
                    while (i < 100) {
                        sendNotice(notifyId = id, content = "${i}%")
                        i += 10
                        delay(500)
                    }
                }
            }
        }

        SpacerV()

        ClickButton("获取手机号") {
            callback("")
            permission(Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_PHONE_NUMBERS)
                .onPermission("手机状态" to "获取手机号")
                .manualIntent()
                .request {
                    val str = if (it.isAllGranted()) {
                        getPhoneNumbers()?.takeIf { l -> l.isNotEmpty() }?.let { s -> s.joinToString { i -> "$i\n" } } ?: "null"
                    } else {
                        it.toString()
                    }
                    callback(str)
                }
        }

        Text(result)
    }

}

@SuppressLint("MissingPermission")
private fun BaseActivity.sendNotice(title: String = "通知", content: String, notifyId: Int = NoticeHelper.newId, onSend: ((Int) -> Unit)? = null) {
    val channelId = ".notice"
    witNoticePermission(channelId) {
        val res = NoticeHelper.send(
            NotificationCompat.Builder(AppHelper, channelId).setSmallIcon(com.munch1182.p1.R.mipmap.ic_launcher).setContentTitle(title).setContentText(content).setAutoCancel(true),
            notifyId = notifyId,
        )
        onSend?.invoke(res)
    }
}

private fun BaseActivity.witNoticePermission(channelId: String, any: () -> Unit) {
    val permission = {
        val name = "测试通知"
        NoticeHelper.checkOrCreateChannel(channelId, name, "测试通知")
        judge({ !NoticeHelper.checkChannelIsDisable(channelId) }, appSetting()).onIntent("该类型的通知已被关闭，请前往设置-通知管理-通知类别中手动允许${name}类别的通知").request { if (it) any() }
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permission(Manifest.permission.POST_NOTIFICATIONS).onPermission("通知" to "获取通知").manualIntent().request {
            if (it.isAllGranted()) permission()
        }
    } else {
        permission()
    }
}
