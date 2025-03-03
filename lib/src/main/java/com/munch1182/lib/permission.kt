package com.munch1182.lib

import android.content.Intent
import android.net.Uri
import android.provider.Settings
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

    fun collectAll(permissions: Array<String>): Array<Array<String>> {
        val granted = arrayListOf<String>()
        val denied = arrayListOf<String>()
        permissions.forEach { if (check(it)) granted.add(it) else denied.add(it) }
        return arrayOf(granted.toTypedArray(), denied.toTypedArray())
    }
}

val appDetailsPage: Intent
    get() = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", AppHelper.packageName, null)
    )