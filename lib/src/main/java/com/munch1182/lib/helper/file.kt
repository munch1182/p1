package com.munch1182.lib.helper

import android.net.Uri
import androidx.core.content.FileProvider
import com.munch1182.lib.AppHelper
import com.munch1182.lib.base.ctx
import com.munch1182.lib.base.nowStr
import com.munch1182.lib.helper.FileHelper.tryExists
import java.io.Closeable
import java.io.File
import java.io.FileOutputStream

object FileHelper {

    fun newCache(name: String = nowStr("yyyyMMddHHmmss")) = File(AppHelper.cacheDir, name)
    fun newFile(name: String = nowStr("yyyyMMddHHmmss")) = File(AppHelper.filesDir, name)
    fun newCache(vararg name: String) = File(AppHelper.cacheDir, name.joinToString(File.separator))
    fun newFile(vararg name: String) = File(AppHelper.filesDir, name.joinToString(File.separator))

    fun File.tryExists(): Boolean {
        if (exists()) return true
        return if (isDirectory) {
            mkdirs()
        } else {
            (parentFile?.let { if (!it.exists()) it.mkdirs() else true } ?: true) && createNewFile()
        }
    }

    /**
     * 如果该文件或者文件夹不存在(新建不存在的文件会被判断为文件而不是文件夹)，则尝试创建，创建失败则抛出异常
     */
    fun File.sureExists(msg: String = "file exception"): File {
        if (!tryExists()) throw IllegalStateException(msg)
        return this
    }

    /**
     * 需要配置fileprovider，而且path需要在配置文件范围内
     */
    fun uri(path: String): Uri? {
        return FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", File(path))
    }

    fun del(file: File) {
        if (file.exists()) {
            if (file.isDirectory) {
                file.listFiles()?.forEach { del(it) }
            }
            file.delete()
        }
    }
}

fun Closeable.closeQuietly() {
    try {
        close()
    } catch (_: Exception) {
    }
}

class FileWriteHelper {

    private var fos: FileOutputStream? = null
    private var file: File? = null

    fun prepare(file: File = FileHelper.newCache(), delIfExist: Boolean = false): File? {
        if (fos != null) release()
        if (delIfExist && file.exists()) FileHelper.del(file)
        val result = file.tryExists()
        if (result) {
            this.file = file
            fos = file.outputStream()
            return file
        }
        return null
    }

    fun write(data: ByteArray, off: Int = 0, len: Int = data.size) {
        fos?.write(data, off, len)
    }

    fun complete(): File? {
        fos?.flush()
        fos?.closeQuietly()
        fos = null
        return file
    }

    fun release() {
        complete()
    }
}