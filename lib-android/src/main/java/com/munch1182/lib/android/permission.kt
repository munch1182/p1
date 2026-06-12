package com.munch1182.lib.android

import android.app.Activity
import android.content.pm.PackageManager.PERMISSION_GRANTED

/**
 * 判断当前权限是否已经获取
 */
fun String.isPermissionGranted() =
    AppHelper.checkSelfPermission(this) == PERMISSION_GRANTED

/**
 * 判断当前权限是否需要解释
 *
 * 1. 当未请求过权限, 返回false;
 * 2. 当权限被授予后, 返回false;
 * 3. 当请求权限被拒绝但未永久拒绝, 返回true;
 * 4. 当该权限被永久拒绝, 返回false;
 *
 * 因此, 当权限执行请求后, 未被授予的权限此值返回false, 则可判断该权限为永久拒绝;
 */
fun String.isPermissionShouldRationale(act: Activity) =
    act.shouldShowRequestPermissionRationale(this)