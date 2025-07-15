package com.munch1182.lib.helper

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.annotation.IntRange
import com.munch1182.lib.base.ctx
import java.io.ByteArrayOutputStream
import java.io.File

object ImgHelper {
    fun imgCompress(file: File, newFile: File, @IntRange(from = 0, to = 100) quality: Int = 30, sampleSize: Int = 2): File? {
        val opt = BitmapFactory.Options().apply {
            inJustDecodeBounds = false //为true的时候不会真正加载图片，而是得到图片的宽高信息
            inSampleSize = sampleSize
        }
        val bitmap = BitmapFactory.decodeFile(file.absolutePath, opt)
        return bitmap2File(bitmap, newFile, quality)
    }

    fun bitmap2File(bitmap: Bitmap, newFile: File, @IntRange(from = 0, to = 100) quality: Int = 100): File? {
        try {
            ByteArrayOutputStream().use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, it)
                it.writeTo(newFile.outputStream())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
        return newFile
    }

    fun res2File(@DrawableRes resId: Int, newFile: File, resources: Resources = ctx.resources, @IntRange(from = 0, to = 100) quality: Int = 100): File? {
        return bitmap2File(BitmapFactory.decodeResource(resources, resId), newFile, quality)
    }
}