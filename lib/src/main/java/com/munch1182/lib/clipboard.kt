package com.munch1182.lib

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PersistableBundle

// https://developer.android.google.cn/develop/ui/views/touch-and-input/copy-paste?hl=zh-cn#Clipboard
// https://developer.android.google.cn/privacy-and-security/risks/secure-clipboard-handling?hl=zh-cn
object ClipboardHelper {

    val cbm by lazy { AppHelper.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager }

    fun copyFrom(): ClipData.Item? {
        // 权限不足仍会被返回false
        if (cbm?.hasPrimaryClip() == true) {
            return cbm?.primaryClip?.getItemAt(0) ?: return null
        }
        return null
    }

    fun copyFrom2Str() = copyFrom()?.coerceToText(AppHelper)?.toString()

    fun clear() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        cbm?.clearPrimaryClip() ?: Unit
    } else {
        putTo("")
    }

    inline fun <reified T> putTo(data: T, isSensitive: Boolean = false) {
        if (data == null) return
        val clipData = when (T::class) {
            String::class -> ClipData.newPlainText("text", data.toString())
            Uri::class -> ClipData.newUri(AppHelper.contentResolver, "uri", data as Uri)
            Intent::class -> ClipData.newIntent("intent", data as Intent)
            else -> null
        } ?: return
        cbm?.setPrimaryClip(clipData.updateSensitive(isSensitive))
    }

    fun putHtmlTo(data: String, isSensitive: Boolean = false) {
        cbm?.setPrimaryClip(ClipData.newHtmlText("html", data, data).updateSensitive(isSensitive))
    }

    fun ClipData.updateSensitive(isSensitive: Boolean = false): ClipData {
        description.extras = PersistableBundle().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, isSensitive)
            } else {
                putBoolean("android.content.extra.IS_SENSITIVE", true)
            }
        }
        return this
    }
}