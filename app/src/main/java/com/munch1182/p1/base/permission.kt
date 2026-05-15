package com.munch1182.p1.base

import android.Manifest
import android.content.Intent
import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.munch1182.core.android.IPrompt
import com.munch1182.core.android.awaitResult
import com.munch1182.core.android.isLocationEnable
import com.munch1182.core.android.isPermissionGranted
import com.munch1182.core.android.locationEnableIntent
import com.munch1182.core.android.result.PermissionDialogProvider
import com.munch1182.core.android.result.PermissionDialogTarget
import com.munch1182.core.android.result.PermissionPrompt
import com.munch1182.core.android.result.isAllGranted
import com.munch1182.core.android.result.requestPermissionWithHelper
import com.munch1182.core.android.result.requestResult
import com.munch1182.core.common.launchMain
import com.munch1182.p1.dialog.Dialog
import com.munch1182.p1.ui.theme.paddingPage

val currAsFragmentActivityOrThrow get() = currOrThrow as? FragmentActivity ?: error("cannot use curr or as FragmentActivity")


private val appSetting
    get() = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = "package:${currOrThrow.packageName}".toUri()
    }

/**
 * 检查蓝牙权限, 已处理兼容
 */
fun checkBluetoothPermission(
    act: FragmentActivity = currAsFragmentActivityOrThrow, granted: (Boolean) -> Unit
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val permission = arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        if (permission.all { it.isPermissionGranted() }) return granted.invoke(true)
        act.lifecycleScope.launchMain {
            val result = act.requestPermissionWithHelper(permission) //
                .dialogProvider(onPermission("蓝牙", "扫描和查找附近蓝牙设备")) //
                .settingIntent(appSetting) //
                .request() //
                .isAllGranted() //
            granted.invoke(result)
        }
    } else {
        act.lifecycleScope.launchMain {
            if (!isLocationEnable()) {
                val isAllow = Dialog.newYesNoDialog("蓝牙需要开启定位权限", "定位权限").awaitResult()
                if (!isAllow) return@launchMain granted.invoke(false)
                act.requestResult(locationEnableIntent)
                if (!isLocationEnable()) return@launchMain granted.invoke(false)
            }
            val locPermission = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            if (!locPermission.all { it.isPermissionGranted() }) {
                val locGranted = act.requestPermissionWithHelper(locPermission) //
                    .dialogProvider(onPermission("定位", "扫描和查找附近蓝牙设备")) //
                    .settingIntent(appSetting) //
                    .request() //
                    .isAllGranted() //
                if (!locGranted) return@launchMain granted.invoke(false)
            }
            val bluePermission = arrayOf(Manifest.permission.BLUETOOTH)
            if (!bluePermission.all { it.isPermissionGranted() }) { //
                val result = act.requestPermissionWithHelper(bluePermission) //
                    .dialogProvider(onPermission("蓝牙", "扫描/连接蓝牙设备")) //
                    .settingIntent(appSetting) //
                    .request() //
                    .isAllGranted() //
                return@launchMain granted.invoke(result)
            }
            return@launchMain granted.invoke(true)
        }
    }
}


private fun newPermissionRequestDialog(msg: String, title: String = "权限请求") = object : PermissionPrompt {
    private val dialog = Dialog.newYesNoDialog(msg, title, ok = "允许", cancel = "拒绝")
    override suspend fun onBeforeRequest() = dialog.awaitResult()
}

private fun newPermissionSettingOpenDialog(msg: String, title: String = "权限请求") = object : PermissionPrompt {
    private val dialog = Dialog.newYesNoDialog(msg, title, ok = "前往", cancel = "算了")
    override suspend fun onBeforeRequest() = dialog.awaitResult()
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

    private val content
        get() = ctx.findViewById<ViewGroup>(android.R.id.content)
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
                Card {
                    Column(Modifier.paddingPage()) {
                        Text(title)
                        Text(msg)
                    }
                }
            }
            view = this
        }
        return v
    }
}


private fun onPermission(name: String, usage: String): PermissionDialogProvider {
    return { target, _ ->
        when (target) {
            PermissionDialogTarget.BEFORE -> newPermissionUsageDialog(usage, "${name}权限")
            PermissionDialogTarget.DENIED -> newPermissionRequestDialog("正在请求${name}权限, 用于${usage}")
            PermissionDialogTarget.NEVER_ASK_AGAIN -> newPermissionSettingOpenDialog("请前往设置界面手动授予${name}权限")
        }
        null
    }
}