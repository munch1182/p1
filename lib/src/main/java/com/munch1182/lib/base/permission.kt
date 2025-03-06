package com.munch1182.lib.base

import android.app.Activity
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker

fun String.asPermissionCheck() = checkPermission(this)
fun String.asPermissionCheckRationale(act: Activity) =
    act.shouldShowRequestPermissionRationale(this)

// checkAll方法会导致传入的权限没有SDK提示，因此不提供
fun checkPermission(permission: String): Boolean {
    return PermissionChecker.checkSelfPermission(ctx, permission) ==
            PermissionChecker.PERMISSION_GRANTED
}

/**
 * 只有在请求过一次权限后才能正确判断
 * 如果不想传入act，需要反射调用PackManager的同名方法调用
 */
fun Activity.checkPermissionRationale(permission: String) =
    ActivityCompat.shouldShowRequestPermissionRationale(this, permission)