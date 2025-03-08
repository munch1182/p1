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
 * 其作用是在请求权限后如果还返回false则是被永久拒绝
 * 如果不想传入act，则需要反射调用PackManager的同名方法调用
 *
 * 例外情形：
 * 如果系统权限弹窗被取消(点击其它地方取消弹窗)，则此方法仍然返回false，根据上面的判断会被误判为永久拒绝(请求后+返回false)
 * 但是基本上认为不需要处理，因为下一次请求权限仍会弹出权限请求，并不影响流程
 */
fun Activity.checkPermissionRationale(permission: String) =
    ActivityCompat.shouldShowRequestPermissionRationale(this, permission)