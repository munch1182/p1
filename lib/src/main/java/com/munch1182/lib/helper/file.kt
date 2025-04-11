package com.munch1182.lib.helper

import android.net.Uri
import androidx.core.content.FileProvider
import com.munch1182.lib.AppHelper
import com.munch1182.lib.base.ctx
import com.munch1182.lib.base.nowStr
import java.io.Closeable
import java.io.File

object FileHelper {

    fun newCache(name: String = nowStr("yyyyMMddHHmmss")) = File(AppHelper.cacheDir, name)
    fun newFile(name: String = nowStr("yyyyMMddHHmmss")) = File(AppHelper.filesDir, name)

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
}

fun Closeable.closeQuietly() {
    try {
        close()
    } catch (_: Exception) {
    }
}