package com.munch1182.p1.base

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.munch1182.lib.helper.currAct

object DialogHelper {

    fun newPermission(
        ctx: Context = currAct, permissionName: String? = null,
        title: String = "权限请求",
        content: String = "请允许${permissionName ?: throw IllegalStateException()}以继续使用应用",
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
}