package com.munch1182.lib.base

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.view.WindowManager
import android.widget.Toast
import com.munch1182.lib.AppHelper


internal val ctx: Context get() = AppHelper

fun Context.findAct(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

fun toast(msg: String) {
    Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
}

inline fun <reified ACT : Activity> Context.startActivity() {
    startActivity(Intent(this, ACT::class.java))
}

val versionName: String?
    get() = packInfo.versionName

val versionCodeCompat: Long
    get() = packInfo.let {
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) it.longVersionCode else it.versionCode.toLong()
    }

val packInfo: PackageInfo get() = ctx.packageManager.getPackageInfo(ctx.packageName, PackageManager.GET_CONFIGURATIONS)

val wm: WindowManager get() = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager