package com.munch1182.p1.helper

import androidx.annotation.WorkerThread
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.munch1182.lib.base.log
import org.jsoup.Jsoup

object NetVideoHelper {

    private val log = log()

    /**
     * 从混杂url的文本中提取第一个url
     */
    fun getFirstUrl(string: String): String? {
        return parseUrl(string).firstOrNull()
    }

    @WorkerThread
    fun parseVideoUrl(url: String): String? {
        val parser: NetParse? = when {
            NetBiliBili.isUrl(url) -> NetBiliBili(url)
            NetDouYin.isUrl(url) -> NetDouYin(url)
            else -> null
        }
        return parser?.parse()
    }

    private class NetBiliBili(private val url: String) : NetParse {
        companion object {
            fun isUrl(url: String): Boolean {
                return url.contains("bilibili.com") || url.contains("https://b23.tv")
            }
        }

        class WindowPlayInfo(val data: Data?) {
            class Data(val dash: Dash?, @SerializedName("accept_quality") val acceptQuality: List<Int>?)
            class Dash(val video: List<Video>?, val audio: List<Audio>?)
            class Video(val baseUrl: String?)
            class Audio(val baseUrl: String?)

            override fun toString(): String {
                return "($title: acceptQuality:[${data?.acceptQuality?.joinToString()}], url: (${data?.dash?.video?.size}, ${data?.dash?.audio?.size}))"
            }

            val url get() = data?.dash?.video?.firstOrNull()?.baseUrl
            var title: String? = null
        }

        override fun parse(): String? {
            log.logStr("parse: url: $url")
            val doc = try {
                Jsoup.connect(url).timeout(3000).get()
            } catch (e: Exception) {
                e.printStackTrace()
                log.logStr("err: $e")
                return null
            } ?: return null
            log.logStr("--------------------")

            val text = doc.html()
            val title = "title=\"(.*?)\"".toRegex().find(text)?.value ?: doc.title()
            val info = "window.__playinfo__=(.*?)</script>".toRegex().find(text)?.value

            if (info != null) {
                val json = info.replace("window.__playinfo__=", "").replace("</script>", "")
                val windowPlayInfo = Gson().fromJson(json, WindowPlayInfo::class.java)
                windowPlayInfo?.title = title
                log.logStr("parse: $windowPlayInfo")
                return windowPlayInfo?.url
            }
            return null
        }
    }

    private class NetDouYin(private val url: String) : NetParse {

        companion object {
            fun isUrl(url: String): Boolean {
                return url.contains("douyin.com")
            }
        }

        override fun parse(): String? {
            return null
        }
    }

    private fun parseUrl(text: String): List<String> {
        val regex = """\b((?:https?|ftp|file)://|www\.)[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|]""".toRegex()

        return regex.findAll(text).map { match ->
            // 为 www 开头的网址自动添加 https:// 协议
            if (match.value.startsWith("www.")) "https://${match.value}" else match.value
        }.filter { !it.contains(",") }.toList()
    }

    private fun interface NetParse {
        fun parse(): String?
    }
}