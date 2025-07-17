package com.munch1182.p1.helper

import android.webkit.WebView
import androidx.annotation.WorkerThread

object NetVideoHelper {

    /**
     * 从混杂url的文本中提取第一个url
     */
    fun getFirstUrl(string: String): String? {
        return parseUrl(string).firstOrNull()
    }

    @WorkerThread
    suspend fun parseVideoUrl(url: String, web: WebView): String? {
        val parser: NetParse? = when {
            NetBiliBili.isUrl(url) -> NetBiliBili(url)
            NetDouYin.isUrl(url) -> NetDouYin(url, web)
            NetXHS.isUrl(url) -> NetXHS(url)
            else -> null
        }
        return parser?.parse()
    }

    private fun parseUrl(text: String): List<String> {
        val regex = """\b((?:https?|ftp|file)://|www\.)[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|]""".toRegex()

        return regex.findAll(text).map { match ->
            // 为 www 开头的网址自动添加 https:// 协议
            if (match.value.startsWith("www.")) "https://${match.value}" else match.value
        }.filter { !it.contains(",") }.toList()
    }

    internal fun interface NetParse {
        suspend fun parse(): String?
    }
}