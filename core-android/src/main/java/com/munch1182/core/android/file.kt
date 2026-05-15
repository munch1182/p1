package com.munch1182.core.android

import androidx.core.content.FileProvider
import com.munch1182.core.common.newPath
import java.io.File


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
fun fileUri(file: File, authority: String = AppHelper.packageName) =
    FileProvider.getUriForFile(AppHelper, authority, file)
