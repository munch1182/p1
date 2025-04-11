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
 * 原意作为是否解释权限用途的判断，google认为权限应该在第一次被拒绝第二次被永久拒绝之前提醒
 * 所以只会在这个时机返回true，其余时机返回false，包括请求前，被拒绝后返回false
 *
 * 但实践中有些权限需要请求之前解释（比如蓝牙相关的定位权限），还需要处理当权限永久拒绝后的跳转设置打开权限处理
 * 所以此方法也可以用于权限被永久拒绝的判断：即在权限实际请求后此方法返回true即被永久拒绝
 *
 * 但是，当权限请求被取消时（通过点击系统权限请求框外），此方法仍会返回false，根据上诉判断此时会被判断为永久拒绝，此为bug，且无法修复。目前只能置之不理。
 */
fun Activity.checkPermissionRationale(permission: String) =
    ActivityCompat.shouldShowRequestPermissionRationale(this, permission)