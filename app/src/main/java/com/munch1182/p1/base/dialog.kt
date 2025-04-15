package com.munch1182.p1.base

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.munch1182.lib.helper.currAct
import com.munch1182.lib.helper.result.AllowDenyDialogContainer
import com.munch1182.lib.helper.result.JudgeHelper
import com.munch1182.lib.helper.result.JudgeHelper.IntentCanLaunchDialogProvider
import com.munch1182.lib.helper.result.PermissionHelper
import com.munch1182.lib.helper.result.PermissionHelper.PermissionCanRequestDialogProvider
import com.munch1182.lib.helper.result.asAllowDenyDialog

object DialogHelper {

    fun newPermissionIntent(
        ctx: Context = currAct, permissionName: String? = null,
        title: String = "权限请求",
        content: String = "请前往设置界面手动允许${permissionName ?: throw IllegalStateException()}权限",
        sure: String = "确定",
        cancel: String = "取消"
    ): AlertDialog {
        return AlertDialog.Builder(ctx)
            .setTitle(title)
            .setMessage(content)
            .setPositiveButton(sure) { _, _ -> }
            .setNegativeButton(cancel) { _, _ -> }
            .create()
    }


    fun intentDialog(before: String?, after: String?): IntentCanLaunchDialogProvider {
        return IntentCanLaunchDialogProvider { _, state ->
            val msg = when (state) {
                JudgeHelper.State.Before -> before
                JudgeHelper.State.After -> after
            } ?: return@IntentCanLaunchDialogProvider null
            allowDenyDialog(msg, "前往授权", "前往")
        }
    }

    fun intentBlueScan(): IntentCanLaunchDialogProvider {
        return IntentCanLaunchDialogProvider { _, _ -> allowDenyDialog("蓝牙扫描附近设备，需要定位功能及权限，请打开定位功能", "权限申请", "去打开") }
    }

    fun permissionBlueScan(): PermissionCanRequestDialogProvider {
        return PermissionCanRequestDialogProvider { _, state, _ ->
            val (msg, ok) = when (state) {
                PermissionHelper.State.DeniedForever -> "定位权限已被拒绝, 需要前往设置页面手动开启定位权限, 否则无法扫描到蓝牙设备。" to "去开启"
                else -> "蓝牙扫描附近设备，需要定位权限，请允许定位权限" to "允许"
            }
            allowDenyDialog(msg, "蓝牙扫描权限", ok)
        }
    }

    fun permissionDialog(name: String, toUse: String, noBefore: Boolean = true, noDenied: Boolean = false): PermissionCanRequestDialogProvider {
        return PermissionCanRequestDialogProvider { _, state, _ ->
            val (msg, ok) = when (state) {
                PermissionHelper.State.Before -> if (noBefore) return@PermissionCanRequestDialogProvider null else "正在申请${name}相关权限, 用于${toUse}。" to "允许"
                PermissionHelper.State.Denied -> if (noDenied) return@PermissionCanRequestDialogProvider null else "正在申请${name}相关权限, 用于${toUse}。" to "允许"
                PermissionHelper.State.DeniedForever -> "该权限已被拒绝, 需要前往设置页面手动开启${name}权限, 否则该功能无法使用。" to "去开启"
            }
            allowDenyDialog(msg, "权限申请", ok)
        }
    }

    private fun allowDenyDialog(msg: String, title: String, sure: String = "确定"): AllowDenyDialogContainer {
        return AlertDialog.Builder(currAct).setTitle(title)
            .setMessage(msg)
            .setPositiveButton(sure) { _, _ -> }
            .setNegativeButton("拒绝") { _, _ -> }
            .create().asAllowDenyDialog()
    }
}