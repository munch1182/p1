package com.munch1182.lib.scan

import com.munch1182.lib.helper.FileHelper
import org.opencv.wechat_qrcode.WeChatQRCode
import java.io.File

object WechatQr {
    private val toFile = FileHelper.newFile("qr_module.zip")
    private val dir = FileHelper.newFile("qr_module_unzip")

    fun new(): WeChatQRCode {
        if (!File(file("detect_prototxt")).exists()) {
            FileHelper.copyAssets("qr_module.zip", toFile.path)
            dir.mkdirs()
            FileHelper.unzip(toFile, dir)
        }

        return WeChatQRCode(
            file("detect_prototxt"),
            file("detect_caffemodel"),
            file("sr_prototxt"),
            file("sr_caffemodel")
        )
    }

    private fun file(name: String): String {
        return "${dir.absolutePath}/qr_module/$name"
    }
}