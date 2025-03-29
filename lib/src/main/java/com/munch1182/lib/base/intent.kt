package com.munch1182.lib.base

import android.content.Intent
import android.net.Uri
import android.provider.Settings

fun Intent.wPName() = setData(Uri.fromParts("package", ctx.packageName, null))

fun shareTextIntent(text: String): Intent = Intent.createChooser(
    Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }, null
)

fun appSetting() = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).wPName()