package com.munch1182.lib.base

import android.content.res.Resources
import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.annotation.ArrayRes
import androidx.annotation.ColorInt


@get:ColorInt
val colorPrimary: Int
    get() = getAttrArrayFromTheme(android.R.attr.colorPrimary) { getColor(0, Color.WHITE) }

val selectableItemBackground: Drawable?
    get() = getAttrArrayFromTheme(android.R.attr.selectableItemBackground) { getDrawable(0) }

fun <T> getAttrArrayFromTheme(attrId: Int, get: TypedArray.() -> T): T {
    val typedArray = ctx.theme.obtainStyledAttributes(intArrayOf(attrId))
    val value = get.invoke(typedArray)
    typedArray.recycle()
    return value
}

/**
 * 使用string-array数组时，使用此方法获取数组
 */
fun getStrArray(@ArrayRes arrayId: Int): Array<out String>? {
    return try {
        ctx.resources.getStringArray(arrayId)
    } catch (e: Resources.NotFoundException) {
        null
    }
}

/**
 * 使用integer-array数组时，使用此方法获取数组
 */
fun getIntArray(@ArrayRes arrayId: Int): IntArray? {
    return try {
        ctx.resources.getIntArray(arrayId)
    } catch (e: Resources.NotFoundException) {
        null
    }
}

/**
 * 使用array数组且item为资源id时，使用此方法获取id数组
 */
fun getIdsArray(@ArrayRes arrayId: Int): IntArray {
    val ota = ctx.resources.obtainTypedArray(arrayId)
    val size = ota.length()
    val array = IntArray(size) { ota.getResourceId(it, 0) }
    ota.recycle()
    return array
}