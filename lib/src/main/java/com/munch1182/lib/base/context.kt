package com.munch1182.lib.base

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
import android.view.WindowManager
import android.widget.Toast
import com.munch1182.lib.AppHelper


internal val ctx: Context
    get() = AppHelper

fun Context.findActivity(): Activity? {
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

fun Context.startActivity(target: Class<out Activity>, bundle: Bundle? = null) =
    startActivity(Intent(this, target).apply {
        val extras = bundle ?: return@apply
        putExtras(extras)
    })

inline fun <reified ACT : Activity> Context.startActivity() {
    startActivity(Intent(this, ACT::class.java))
}

fun Intent.withPName() = setData(Uri.fromParts("package", ctx.packageName, null))

val versionName: String?
    get() = packInfo.versionName

@Suppress("DEPRECATION")
val versionCodeCompat: Long
    get() = packInfo.let { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) it.longVersionCode else it.versionCode.toLong() }

val packInfo: PackageInfo
    get() = ctx.packageManager.getPackageInfo(ctx.packageName, PackageManager.GET_CONFIGURATIONS)

val wm: WindowManager
    get() = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager

val appDetailsPage: Intent
    get() = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).withPName()

fun Int.dp2Px() = ctx.resources.displayMetrics.density * this
fun Int.sp2Px() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, this.toFloat(), ctx.resources.displayMetrics).toInt()