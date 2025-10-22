package com.munch1182.lib.helper

import android.net.Uri
import androidx.core.content.FileProvider
import com.munch1182.lib.AppHelper
import okio.Closeable
import java.io.File
import java.io.FileOutputStream

object FileHelper {
    fun newCache(vararg name: String) = File(AppHelper.cacheDir, name.joinToString(separator = "") { "${File.separator}$it" })
    fun newFile(vararg name: String) = File(AppHelper.filesDir, name.joinToString(separator = "") { "${File.separator}$it" })

    /**
     * 获取文件对应的Uri
     *
     * 需要定义FileProvider
     */
    fun uri(file: File?): Uri? = file?.let { runCatching { FileProvider.getUriForFile(AppHelper, AppHelper.packageName, it) }.getOrNull() }
}

fun File.createParents() = try {
    parentFile?.let { it.exists() || it.mkdirs() } ?: true
} catch (e: Exception) {
    e.printStackTrace()
    false
}

/**
 * 如果该文件/文件夹已存在，则返回true
 * 否则，根据文件名是否有后缀来进行创建文件或者文件夹
 */
fun File.createIfNotExist(): Boolean {
    try {
        if (exists()) return true
        return createParents() && (if (extension.isEmpty()) mkdir() else createNewFile())
    } catch (e: Exception) {
        e.printStackTrace()
        return false
    }
}

fun File.createNew(): Boolean {
    if (exists() && !delAll()) return false
    return createIfNotExist()
}

fun File.delAll(): Boolean {
    if (!exists()) return true
    var result = true
    if (isDirectory) listFiles()?.forEach { result = result && it.delAll() }
    return result && delete()
}

/**
 * 文件写入服务类
 *
 * @see prepare 准备文件
 * @see write 写入数据
 * @see complete 完成写入，刷新并关闭写入
 */
class FileWriteHelper : Closeable {

    private var fos: FileOutputStream? = null
    private var file: File? = null

    fun prepare(file: File, delIfExist: Boolean = false): File? {
        if (delIfExist && file.exists()) file.delAll()
        if (file.createNew()) {
            this.file = file
            fos = FileOutputStream(file)
            return this.file
        }
        return null
    }

    fun write(data: ByteArray, off: Int = 0, len: Int = data.size) {
        fos?.write(data, off, len)
    }

    fun complete(): File? {
        fos?.let {
            it.flush()
            it.close()
        }
        fos = null
        return file
    }

    override fun close() {
        complete()
    }
}