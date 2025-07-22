package com.munch1182.p1.helper

import android.webkit.WebView
import com.munch1182.lib.base.log
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

class NetKS(private val url: String, private val web: WebView) : NetVideoHelper.NetParse {

    companion object {
        fun isUrl(url: String): Boolean {
            return url.contains("kuaishou") || url.contains("chenzhongtech")
        }
    }

    private val log = log()

    private val net = OkHttpClient()
    private val headers by lazy {
        Headers.headersOf(
            "User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3",
        )
    }

    override suspend fun parse(): String? {

        log.logStr("parse: url: $url")
        val req = Request.Builder().url(url).method("HEAD", null).headers(headers).build()

        val response = try {
            net.newCall(req).execute()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } ?: return null

        val resUrl = response.request.url.toString()
        log.logStr("resUrl: $resUrl")
        val id = getId(resUrl) ?: return null
        log.logStr("id: $id")

        val doc = Jsoup.connect(url).timeout(3000).get().html()

        val info = "window.__APOLLO_STATE__=(.*?)</script>".toRegex().find(doc)?.value

        if (info != null) {
            val json = info.replace("window.__APOLLO_STATE__=", "").replace("</script>", "").replace(";(function(){var s;(s=document.currentScript||document.scripts[document.scripts.length-1]).parentNode.removeChild(s);}());", "")

            log.logStrSplit(json)

            val url = "\"photoUrl\":(.*?),".toRegex().find(json)?.value?.replace("\"photoUrl\":", "")?.replace(",", "")?.removePrefix("\"")?.removeSuffix("\"")
            log.logStr("url: $url")

            return url
        }
        return null
    }

    private fun getId(url: String): String? {
        return if (url.contains("chenzhongtech")) {
            url.split("?")[1].split("&").firstOrNull { it.startsWith("photoId=") }?.let { it.split("=")[1] }
        } else if (url.contains("short-video")) {
            if (url.contains("?")) url.split("?")[0].split("/")[4] else url.split("/")[4]
        } else {
            null
        }
    }
}