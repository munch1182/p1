package com.munch1182.lib

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker

object PermissionHelper {

    fun check(permission: String): Boolean {
        return PermissionChecker
            .checkSelfPermission(AppHelper, permission) ==
                PermissionChecker.PERMISSION_GRANTED
    }

    fun checkAll(permissions: Array<String>): Boolean {
        permissions.forEach { if (check(it)) return false }
        return true
    }

    fun collectDenied(permissions: Array<String>): Array<String> {
        return permissions.filter { !check(it) }.toTypedArray()
    }

    fun collectGranted(permissions: Array<String>): Array<String> {
        return permissions.filter { check(it) }.toTypedArray()
    }

    fun collectAll(permissions: Array<String>): PermissionResult {
        val result = PermissionResult()
        permissions.forEach { if (check(it)) result.grant.add(it) else result.deny.add(it) }
        return result
    }

    /**
     * 只有在请求过一次权限后才能正确判断
     */
    fun collectAllRationale(
        fa: Activity,
        permissions: Array<String>
    ): PermissionRationaleResult {
        val result = PermissionRationaleResult()
        permissions.forEach {
            if (check(it)) {
                result.grant.add(it)
            } else if (checkRationale(fa, it)) {
                result.rationale_.add(it)
            } else {
                result.deny.add(it)
            }
        }
        return result
    }

    /**
     * 只有在请求过一次权限后才能正确判断
     */
    fun checkRationale(fa: Activity, permission: String) =
        ActivityCompat.shouldShowRequestPermissionRationale(fa, permission)
}

open class PermissionResult {
    internal val grant = arrayListOf<String>()
    internal val deny = arrayListOf<String>()
    val granted: Array<String>
        get() = grant.toTypedArray()
    val denied: Array<String>
        get() = deny.toTypedArray()
}

class PermissionRationaleResult : PermissionResult() {
    internal val rationale_ = arrayListOf<String>()
    val rationale: Array<String>
        get() = rationale_.toTypedArray()
}

val appDetailsPage: Intent
    get() = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", AppHelper.packageName, null)
    )