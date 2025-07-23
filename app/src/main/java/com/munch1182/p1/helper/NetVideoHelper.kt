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
    suspend fun parseVideoUrl(url: String, web: WebView): ParseResult? {
        val parser: NetParse? = when {
            NetBiliBili.isUrl(url) -> NetBiliBili(url)
            NetDouYin.isUrl(url) -> NetDouYin(url, web)
            NetXHS.isUrl(url) -> NetXHS(url)
            NetKS.isUrl(url) -> NetKS(url, web)
            else -> null
        }
        return try {
            parser?.parse()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseUrl(text: String): List<String> {
        val regex = """\b((?:https?|ftp|file)://|www\.)[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|]""".toRegex()

        return regex.findAll(text).map { match ->
            // 为 www 开头的网址自动添加 https:// 协议
            if (match.value.startsWith("www.")) "https://${match.value}" else match.value
        }.filter { !it.contains(",") }.toList()
    }

    class ParseResult(val title: String?, val url: String?, val isVideo: Boolean = false) {

        val isSuccess get() = url != null
    }

    internal fun interface NetParse {
        suspend fun parse(): ParseResult?
    }
}