package com.munch1182.core.ui.permission

import android.Manifest
import android.bluetooth.BluetoothManager
import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.munch1182.core.ui.currAsFragmentActivityOrThrow
import com.munch1182.core.ui.dialog.DialogFactory
import com.munch1182.core.ui.theme.paddingPage
import com.munch1182.lib.android.IPrompt
import com.munch1182.lib.android.awaitResult
import com.munch1182.lib.android.enableNotificationIntent
import com.munch1182.lib.android.hasNotificationPermission
import com.munch1182.lib.android.isLocationEnable
import com.munch1182.lib.android.isNotificationEnable
import com.munch1182.lib.android.isPermissionGranted
import com.munch1182.lib.android.locationEnableIntent
import com.munch1182.lib.android.result.PermissionDialogProvider
import com.munch1182.lib.android.result.PermissionDialogTarget
import com.munch1182.lib.android.result.PermissionPrompt
import com.munch1182.lib.android.result.isAllGranted
import com.munch1182.lib.android.result.requestPermissionWithHelper
import com.munch1182.lib.android.result.requestResult
import com.munch1182.lib.common.AnalyticsTracker
import com.munch1182.lib.common.launchMain

/**
 * 检查蓝牙权限，兼容不同 Android 版本。
 */
fun checkBluetoothPermission(
    act: FragmentActivity = currAsFragmentActivityOrThrow,
    analytics: AnalyticsTracker? = null,
    granted: (Boolean) -> Unit
) {
    val onGranted = { result: Boolean ->
        analytics?.trackEvent("checkBluetoothPermission", mapOf("授权状态" to result))
        granted.invoke(result)
    }

    act.lifecycleScope.launchMain {
        val bm = act.getSystemService(BluetoothManager::class.java)
        if (!bm.adapter.isEnabled) {
            act.requestResult(enableNotificationIntent)
            if (!bm.adapter.isEnabled) return@launchMain onGranted.invoke(false)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permission = arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
            if (permission.all { it.isPermissionGranted() }) return@launchMain onGranted.invoke(true)
            val result = act.requestPermissionWithHelper(permission)
                .dialogProvider(onNormalPermission("蓝牙", "扫描和查找附近蓝牙设备"))
                .settingIntent()
                .request()
                .isAllGranted()
            onGranted.invoke(result)
        } else {
            if (!isLocationEnable()) {
                val isAllow = DialogFactory.newYesNoDialog("蓝牙需要开启定位权限", "定位权限").awaitResult() ?: false
                if (!isAllow) return@launchMain onGranted.invoke(false)
                act.requestResult(locationEnableIntent)
                if (!isLocationEnable()) return@launchMain onGranted.invoke(false)
            }
            val locPermission = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            if (!locPermission.all { it.isPermissionGranted() }) {
                val locGranted = act.requestPermissionWithHelper(locPermission)
                    .dialogProvider(onNormalPermission("定位", "扫描和查找附近蓝牙设备"))
                    .settingIntent()
                    .request()
                    .isAllGranted()
                if (!locGranted) return@launchMain onGranted.invoke(false)
            }
            val bluePermission = arrayOf(Manifest.permission.BLUETOOTH)
            if (!bluePermission.all { it.isPermissionGranted() }) {
                val result = act.requestPermissionWithHelper(bluePermission)
                    .dialogProvider(onNormalPermission("蓝牙", "扫描/连接蓝牙设备"))
                    .settingIntent()
                    .request()
                    .isAllGranted()
                return@launchMain onGranted.invoke(result)
            }
            return@launchMain onGranted.invoke(true)
        }
    }
}

/**
 * 检查通知权限，兼容不同 Android 版本。
 */
fun checkNotificationPermission(
    act: FragmentActivity = currAsFragmentActivityOrThrow,
    granted: (Boolean) -> Unit
) {
    if (hasNotificationPermission()) {
        return granted(true)
    } else if (!isNotificationEnable) {
        act.lifecycleScope.launchMain {
            val result = DialogFactory.newYesNoDialog("请前往打开通知", "通知开关").awaitResult() ?: false
            if (result) act.requestResult(enableNotificationIntent)
            if (isNotificationEnable) {
                checkNotificationPermission(act, granted)
            } else {
                granted(false)
            }
        }
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        act.lifecycleScope.launchMain {
            val result = act.requestPermissionWithHelper(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                .dialogProvider(onNormalPermission("通知", "通知权限"))
                .settingIntent()
                .request()
            granted(result.isAllGranted())
        }
    } else {
        granted(true)
    }
}

private fun newPermissionRequestDialog(msg: String, title: String = "权限请求") = object : PermissionPrompt {
    private val dialog = DialogFactory.newYesNoDialog(msg, title, ok = "允许", cancel = "拒绝")
    override suspend fun onBeforeRequest() = dialog.awaitResult() ?: false
}

private fun newPermissionSettingOpenDialog(msg: String, title: String = "权限请求") = object : PermissionPrompt {
    private val dialog = DialogFactory.newYesNoDialog(msg, title, ok = "前往", cancel = "算了")
    override suspend fun onBeforeRequest() = dialog.awaitResult() ?: false
}

private fun newPermissionUsageDialog(msg: String, title: String = "权限使用") = object : PermissionPrompt {
    private val notice = NewPermissionNoticeHelper(msg, title)
    override suspend fun onBeforeRequest(): Boolean {
        notice.show()
        return true
    }

    override suspend fun onAfterRequest() {
        notice.dismiss()
    }
}

private class NewPermissionNoticeHelper(
    private val msg: String,
    private val title: String,
    private val ctx: FragmentActivity = currAsFragmentActivityOrThrow,
) : IPrompt {

    private val content get() = ctx.findViewById<ViewGroup>(android.R.id.content)
    private var view: View? = null

    override fun show() {
        val view = createViewIfNeed()
        content.addView(view)
    }

    override fun dismiss() {
        view?.let { content.removeView(it) }
        view = null
    }

    private fun createViewIfNeed(): View {
        val v = view ?: ComposeView(ctx).apply {
            setContent {
                Box(
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                ) {
                    Card(
                        Modifier
                            .paddingPage()
                            .wrapContentHeight()
                    ) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                                .paddingPage()
                        ) {
                            Text(title, style = MaterialTheme.typography.titleMedium)
                            Text(msg, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            view = this
        }
        return v
    }
}

private fun onNormalPermission(name: String, usage: String): PermissionDialogProvider {
    return { target, _ ->
        when (target) {
            PermissionDialogTarget.BEFORE -> newPermissionUsageDialog(usage, "${name}权限")
            PermissionDialogTarget.DENIED -> newPermissionRequestDialog("正在请求${name}权限, 用于${usage}")
            PermissionDialogTarget.NEVER_ASK_AGAIN -> newPermissionSettingOpenDialog("请前往设置界面手动授予${name}权限")
        }
    }
}
