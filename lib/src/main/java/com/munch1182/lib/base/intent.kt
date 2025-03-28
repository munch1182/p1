package com.munch1182.lib.base

import android.content.Intent
import android.net.Uri

fun Intent.wPName() = setData(Uri.fromParts("package", ctx.packageName, null))

fun shareTextIntent(text: String): Intent = Intent.createChooser(
    Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }, null
)