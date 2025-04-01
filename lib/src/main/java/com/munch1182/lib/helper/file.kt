package com.munch1182.lib.helper

import com.munch1182.lib.AppHelper
import com.munch1182.lib.base.nowStr
import java.io.File

object FileHelper {

    fun newCache(name: String = nowStr("yyyyMMddHHmmss")) = File(AppHelper.cacheDir, name)

    fun File.sureExists(): Boolean {
        if (exists()) return true
        return if (isDirectory) {
            mkdirs()
        } else {
            (parentFile?.mkdirs() ?: true) && createNewFile()
        }
    }
}