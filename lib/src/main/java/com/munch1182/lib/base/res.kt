package com.munch1182.lib.base

import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.MediaStore
import androidx.annotation.ArrayRes
import androidx.annotation.ColorInt
import androidx.annotation.StringRes

@get:ColorInt
val colorPrimary: Int get() = getAttrArrayFromTheme(android.R.attr.colorPrimary) { getColor(0, Color.WHITE) }

val selectableItemBackground: Drawable? get() = getAttrArrayFromTheme(android.R.attr.selectableItemBackground) { getDrawable(0) }

fun <T> getAttrArrayFromTheme(attrId: Int, get: TypedArray.() -> T): T {
    return ctx.theme.obtainStyledAttributes(intArrayOf(attrId)).use { get.invoke(it) }
}

/**
 * 使用string-array数组时，使用此方法获取数组
 */
fun getStrArray(@ArrayRes arrayId: Int): Array<out String>? {
    return runCatching { ctx.resources.getStringArray(arrayId) }.getOrNull()
}

fun str(@StringRes str: Int) = ctx.getString(str)

/**
 * 使用integer-array数组时，使用此方法获取数组
 */
fun getIntArray(@ArrayRes arrayId: Int): IntArray? {
    return runCatching { ctx.resources.getIntArray(arrayId) }.getOrNull()
}

/**
 * 使用array数组且item为资源id时，使用此方法获取id数组
 */
fun getIdsArray(@ArrayRes arrayId: Int): IntArray {
    return ctx.resources.obtainTypedArray(arrayId).use {
        IntArray(it.length()) { i -> it.getResourceId(i, 0) }
    }
}

fun Uri.getPath(proj: Array<String>, columnName: String): String? {
    var res: String? = null
    runCatching {
        val cursor = ctx.contentResolver.query(this, proj, null, null, null)
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndexOrThrow(columnName)
                res = cursor.getString(columnIndex)
            }
        }
        cursor?.close()
    }
    return res
}

fun Uri.getMediaPath() = getPath(arrayOf(MediaStore.Images.Media.DATA), MediaStore.Images.Media.DATA)