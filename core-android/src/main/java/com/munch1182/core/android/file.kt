package com.munch1182.core.android

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
