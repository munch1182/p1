package com.munch1182.lib.android

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.munch1182.lib.common.newPath
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException


/**
 * 创建新文件的辅助函数
 * @param path 可变参数，表示文件的路径组成部分
 * @return 返回组合后的完整路径
 */
fun newFile(vararg path: String) = newPath(AppHelper.filesDir, *path)

/**
 * 创建一个新的缓存文件路径
 * @param path 可变参数，表示相对于缓存目录的子路径
 * @return 返回一个File对象，表示完整的缓存文件路径
 */
fun newCache(vararg path: String): File = newPath(AppHelper.cacheDir, *path)

/**
 * 创建一个文件的 uri 路径
 *
 * 要求：必须在 AndroidManifest.xml 中注册 Provider，并在 xml 中添加文件的路径。
 *
 * 1. AndroidManifest.xml 中注册 FileProvider（放在 `<application>` 标签内）：
 * ```xml
 * <provider
 *     android:name="androidx.core.content.FileProvider"
 *     android:authorities="${applicationId}"
 *     android:exported="false"
 *     android:grantUriPermissions="true">
 *     <meta-data
 *         android:name="android.support.FILE_PROVIDER_PATHS"
 *         android:resource="@xml/file_paths" />
 * </provider>
 * ```
 *
 * 2. res/xml/file_paths.xml 中配置可访问的文件路径（示例：外部存储的 Download 目录）：
 * ```xml
 * <paths>
 *      <!-- 对应 Context.getFilesDir()，即 /data/data/包名/files -->
 *      <files-path
 *          name="my_files"
 *          path="." />   <!-- 整个 files 目录下的所有文件均可访问 -->
 *
 *      <!-- 对应 Context.getCacheDir()，即 /data/data/包名/cache -->
 *      <cache-path
 *          name="my_cache"
 *           path="." />   <!-- 整个 cache 目录下的所有文件均可访问 -->
 * </paths>
 * ```
 *
 * @param file 要共享的文件对象（必须位于已配置的路径下）
 * @param authority provider 的 authorities，默认使用 AppHelper.packageName（需与 manifest 中一致）
 * @return 对应的 content:// Uri
 */
fun fileUri(file: File, authority: String = AppHelper.packageName) = FileProvider.getUriForFile(AppHelper, authority, file)

/**
 * 将URI指向的文件复制到指定目录
 *
 * @param Uri 已经有权限的uri地址
 * @param dir 目标目录
 * @param fileName 目标文件名，如果为null则尝试从URI获取
 * @param overWrite 是否覆盖已存在的文件
 * @return 复制成功返回目标文件，失败返回null
 */
fun Uri.copy2File(dir: File, fileName: String? = null, overWrite: Boolean = true, ctx: Context = AppHelper): File? {
    // 确定文件名，如果未提供则尝试从URI获取，默认为"unknown"
    val fileName = fileName ?: runCatching { ctx.getNameFromUri(this) }.getOrNull() ?: "unknown"
    val targetFile = File(dir, fileName)
    if (targetFile.exists()) {
        if (!overWrite) return null
        targetFile.delete()
    }
    targetFile.parentFile?.mkdirs()
    return try {
        ctx.contentResolver.openInputStream(this)?.use { input ->
            targetFile.outputStream().use { output -> input.copyTo(output) }
        }
        if (targetFile.exists() && targetFile.length() > 0) targetFile else null
    } catch (e: FileNotFoundException) {
        e.printStackTrace()
        null
    } catch (e: IOException) {
        e.printStackTrace()
        targetFile.delete() // 删除可能产生的损坏文件
        null
    } catch (e: SecurityException) {
        e.printStackTrace()
        null
    }
}

/**
 * 获取Uri指向的文件名，如果获取失败则返回null
 */
private fun Context.getNameFromUri(uri: Uri): String? {
    // 如果是 file:// 协议，直接取路径
    if (uri.scheme == "file") {
        return uri.path?.let { File(it).name }
    }
    var name: String? = null
    // 如果是 content://，查询数据库
    contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) name = cursor.getString(0)
    }

    if (name == null || name.contains('.')) { // 不处理复杂情形
        return name
    }

    val mimeType = contentResolver.getType(uri) ?: return name
    val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: return name
    return "${name}.$extension"
}