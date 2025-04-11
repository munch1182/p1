package com.munch1182.lib.helper

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PersistableBundle
import com.munch1182.lib.AppHelper
import com.munch1182.lib.base.ctx

// https://developer.android.google.cn/develop/ui/views/touch-and-input/copy-paste?hl=zh-cn#Clipboard
// https://developer.android.google.cn/privacy-and-security/risks/secure-clipboard-handling?hl=zh-cn
object ClipboardHelper {

    val cbm by lazy { ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager }

    fun copyFrom(): ClipData.Item? {
        // 权限不足仍会被返回false
        if (cbm?.hasPrimaryClip() == true) {
            return kotlin.runCatching { cbm?.primaryClip?.getItemAt(0) ?: return null }.getOrNull()
        }
        return null
    }

    fun copyFrom2Str() = copyFrom()?.coerceToText(ctx)?.toString()

    fun clear() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        cbm?.clearPrimaryClip()
    } else {
        putTo("", "")
    }

    inline fun <reified T> putTo(label: String, data: T, isSensitive: Boolean = false) {
        if (data == null) return
        val clipData = when (T::class) {
            String::class -> ClipData.newPlainText(label, data.toString())
            Uri::class -> ClipData.newUri(AppHelper.contentResolver, label, data as Uri)
            Intent::class -> ClipData.newIntent(label, data as Intent)
            else -> null
        } ?: return
        cbm?.setPrimaryClip(clipData.updateSensitive(isSensitive))
    }

    fun putHtmlTo(label: String, data: String, isSensitive: Boolean = false) {
        cbm?.setPrimaryClip(ClipData.newHtmlText(label, data, data).updateSensitive(isSensitive))
    }

    fun ClipData.updateSensitive(isSensitive: Boolean = false): ClipData {
        if (!isSensitive) return this
        description.extras = PersistableBundle().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            } else {
                putBoolean("android.content.extra.IS_SENSITIVE", true)
            }
        }
        return this
    }
}