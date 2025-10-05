package com.munch1182.lib.base

import androidx.core.content.PermissionChecker
import com.munch1182.lib.AppHelper
import com.munch1182.lib.helper.ActivityCurrHelper


fun String.isGranted() = PermissionChecker.checkSelfPermission(AppHelper, this) == PermissionChecker.PERMISSION_GRANTED
fun String.isCanAsk() = ActivityCurrHelper.curr?.shouldShowRequestPermissionRationale(this) ?: false