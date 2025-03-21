package com.munch1182.lib.helper

import android.content.ContentValues
import android.net.Uri
import android.provider.MediaStore
import androidx.annotation.WorkerThread
import com.munch1182.lib.base.Need
import com.munch1182.lib.base.ctx
import java.io.File
import java.io.OutputStream

object FileHelper {

    @WorkerThread
    fun newAudio(name: String, mineType: String, write: (OutputStream) -> Unit, onSuccess: ((Uri) -> Unit)? = null) {
        val cv = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, mineType)
            put(MediaStore.MediaColumns.IS_PENDING, 1) // 标记写入中
        }

        val cr = ctx.contentResolver
        val uri = cr.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cv) ?: return

        try {
            val os = cr.openOutputStream(uri)
            os?.apply {
                write(this)
                flush()

                cv.clear()
                cv.put(MediaStore.Audio.Media.IS_PENDING, 0) // 写入完成
                cr.update(uri, cv, null, null)

                onSuccess?.invoke(uri)
            }
            os?.close()
        } catch (e: Exception) {
            e.printStackTrace()
            cr.delete(uri, null, null) // 删除无效的uri
        }
    }

    fun newMediaStore(name: String, mineType: String) = MediaStoreHelper(name, mineType)

    fun new(dir: String, fileName: String) = new(File(dir), fileName)

    /**
     * 返回文件，如果文件不存在，创建文件；如果文件已存在且[delIfExists]，删除文件
     */
    fun new(path: String, delIfExists: Boolean = true) = new(File(path), delIfExists)

    fun new(dir: File, fileName: String) = new(dir.resolve(fileName))

    fun new(file: File, delIfExists: Boolean = true): File {
        if (file.parentFile?.exists() != true) file.parentFile?.mkdirs()
        if (file.exists()) {
            if (delIfExists) file.delete()
        } else {
            file.createNewFile()
        }
        return file
    }

    fun newCacheFile(fileName: String) = new(ctx.cacheDir, fileName)

    class MediaStoreHelper(name: String, mineType: String) {
        private val cv = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, mineType)
            put(MediaStore.MediaColumns.IS_PENDING, 1) // 标记写入中
        }
        private val cr = ctx.contentResolver
        private var _uri: Uri? = null
        private var _os: OutputStream? = null

        @Need("Exception Catch")
        fun create() {
            _uri = cr.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cv)
            _os = _uri?.let { cr.openOutputStream(it) }
        }

        @Need("close()")
        fun write(bytes: ByteArray, off: Int = 0, len: Int = bytes.size) {
            _os?.write(bytes, off, len)
        }

        val os: OutputStream?
            get() = _os

        val uri: Uri?
            get() = _uri

        fun close() {
            try {
                if (_os != null) {
                    _os?.flush()
                    cv.clear()
                    cv.put(MediaStore.Audio.Media.IS_PENDING, 0) // 写入完成
                    _uri?.let { cr.update(it, cv, null, null) }
                } else {
                    _uri?.let { cr.delete(it, null, null) } // 删除无效的uri
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

fun Uri.realPath(): String? {
    var res: String? = null
    runCatching {
        val proj = arrayOf(MediaStore.MediaColumns.DATA)

        val cursor = ctx.contentResolver.query(this, proj, null, null, null)
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                res = cursor.getString(columnIndex)
            }
        }
        cursor?.close()
    }
    return res
}

