package com.munch1182.p1.helper

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.munch1182.lib.base.log
import com.munch1182.p1.helper.NetVideoHelper.NetParse
import com.munch1182.p1.helper.NetVideoHelper.ParseResult
import org.jsoup.Jsoup

internal class NetBiliBili(private val url: String) : NetParse {
    companion object {
        fun isUrl(url: String): Boolean {
            return url.contains("bilibili.com") || url.contains("https://b23.tv")
        }
    }

    private val log = log()

    internal class WindowPlayInfo(val data: Data?) {
        class Data(val dash: Dash?, @SerializedName("accept_quality") val acceptQuality: List<Int>?)
        class Dash(val video: List<Video>?, val audio: List<Audio>?)
        class Video(val baseUrl: String?)
        class Audio(val baseUrl: String?)

        override fun toString(): String {
            return "($title: acceptQuality:[${data?.acceptQuality?.joinToString()}], url: (${data?.dash?.video?.size}, ${data?.dash?.audio?.size}))"
        }

        val url get() = data?.dash?.audio?.lastOrNull()?.baseUrl ?: data?.dash?.video?.lastOrNull()?.baseUrl
        var title: String? = null
    }

    override suspend fun parse(): ParseResult? {
        log.logStr("parse: url: $url")
        val doc = Jsoup.connect(url).timeout(3000).get()

        val text = doc.html()
        val title = "title=\"(.*?)\"".toRegex().find(text)?.value ?: doc.title()
        val info = "window.__playinfo__=(.*?)</script>".toRegex().find(text)?.value

        if (info != null) {
            val json = info.replace("window.__playinfo__=", "").replace("</script>", "")
            val windowPlayInfo = Gson().fromJson(json, WindowPlayInfo::class.java)
            windowPlayInfo?.title = title
            log.logStr("parse: $windowPlayInfo")
            val audio = windowPlayInfo?.data?.dash?.audio?.lastOrNull()?.baseUrl
            val video = windowPlayInfo?.data?.dash?.video?.lastOrNull()?.baseUrl
            return ParseResult(windowPlayInfo.title, audio ?: video, audio != null)
        }
        return null
    }
}
