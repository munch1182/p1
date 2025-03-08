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
 * 只有在请求过一次权限后才被拒绝了才会返回true，否则都会(包括不再询问后)返回false
 * 其作用是在请求权限后如果返回false则是被永久拒绝
 * 如果不想传入act，需要反射调用PackManager的同名方法调用
 */
fun Activity.checkPermissionRationale(permission: String) =
    ActivityCompat.shouldShowRequestPermissionRationale(this, permission)