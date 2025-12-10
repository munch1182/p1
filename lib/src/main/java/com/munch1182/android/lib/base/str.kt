package com.munch1182.lib.base

import android.app.Activity
import androidx.core.content.PermissionChecker
import com.munch1182.lib.AppHelper
import com.munch1182.lib.helper.ActivityCurrHelper


fun String.isGranted() = PermissionChecker.checkSelfPermission(AppHelper, this) == PermissionChecker.PERMISSION_GRANTED
fun String.isCanAsk(activity: Activity? = ActivityCurrHelper.curr) = activity?.shouldShowRequestPermissionRationale(this) ?: false