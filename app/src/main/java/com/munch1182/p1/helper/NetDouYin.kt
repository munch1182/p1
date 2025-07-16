package com.munch1182.p1.helper

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.WorkerThread
import com.google.gson.Gson
import com.munch1182.lib.base.log
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlin.coroutines.resume
import kotlin.random.Random

internal class NetDouYin(private val url: String, private val web: WebView) : NetVideoHelper.NetParse {

    companion object {
        fun isUrl(url: String): Boolean {
            return url.contains("douyin.com")
        }
    }

    private val log = log()
    private val net = OkHttpClient()
    private val gson by lazy { Gson() }
    private val headers by lazy {
        Headers.headersOf(
            "User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3",
            "Referer", "https://www.douyin.com/"
        )
    }

    override suspend fun parse(): String? {
        log.logStr("parse: url: $url")
        val req = Request.Builder().url(url).method("HEAD", null).headers(headers).build()
        log.logStr("req: ${req.url}, $headers")

        val response = try {
            net.newCall(req).execute()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } ?: return null

        val resUrl = response.request.url.toString()

        log.logStr("resUrl: $resUrl")

        val videoID = "/video/(\\d+)".toRegex().find(resUrl)?.groupValues?.getOrNull(1) ?: return null
        log.logStr("videoID: $videoID")

        val url = "https://www.douyin.com/aweme/v1/web/aweme/detail/"
        val msToken = generateMsToken()
        val params = Params(videoID, msToken)
        val abogus = try {
            generateAbogus(params).removePrefix("\"").removeSuffix("\"")
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
        params.aBogus = abogus
        val cookie = generateCookie(msToken)

        val finalUrl = "${url}?${params.toUrl()}"
        val finalHead = headers.newBuilder().add("Cookie", cookie).build()

        log.logStr("finalUrl: ${finalUrl.replace("https://www.douyin.com", "")}")
        log.logStr("cookie: ${finalHead["Cookie"]}")
        log.logStr("User-Agent: ${finalHead["User-Agent"]}")
        log.logStr("Referer: ${finalHead["Referer"]}")
        val newReq = Request.Builder().get().url(finalUrl).headers(finalHead).build()

        try {
            val res = net.newCall(newReq).execute()
            log.logStr("url:${res.request.url.toString().replace("https://www.douyin.com", "")}")
            log.logStr("${res.request.header("Cookie")}")
            log.logStr("${res.request.header("User-Agent")}")
            log.logStr("${res.request.header("Referer")}")
            val json = res.body.string()
            log.logStr("json: $json")
            val data = gson.fromJson(json, Response::class.java)
            val title = data.aweme_detail.desc
            val videUrl = data.aweme_detail.video.play_addr.url_list[0]
            log.logStr("$title: $videUrl")
            return videUrl
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    @WorkerThread
    private fun generateCookie(msToken: String): String {
        val ttwid = generateTTWid()
        log.logStr("ttwid: $ttwid")
        return "msToken=$msToken; ttwid=$ttwid;"
    }

    @WorkerThread
    private fun generateTTWid(): String {
        val url = "https://ttwid.bytedance.com/ttwid/union/register/"
        val data = TTWid()
        try {
            val request = Request.Builder().url(url).post(gson.toJson(data).toRequestBody()).build()
            val response = net.newCall(request).execute()
            val cookie = response.headers["Set-Cookie"] ?: return ""
            return "ttwid=(.*?);".toRegex().find(cookie)?.groupValues?.getOrNull(1) ?: ""
        } catch (e: Exception) {
            return ""
        }
    }

    private suspend fun generateAbogus(params: Params): String {
        return suspendCancellableCoroutine { c ->
            web.post {
                web.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        val param = params.toAbogus()
                        val agent = headers["User-Agent"]
                        log.logStr("exaljs ${param}, $agent")
                        view?.evaluateJavascript("javascript:sign(\"${param}\", \"${agent}\")") {
                            log.logStr("abogus: $it")
                            c.resume(it)
                        }
                    }
                }
                web.loadUrl("file:///android_asset/abogus.html")
            }
        }
    }

    private fun generateMsToken(len: Int = 120): String {
        val sb = StringBuilder()
        val baseStr = "ABCDEFGHIGKLMNOPQRSTUVWXYZabcdefghigklmnopqrstuvwxyz0123456789="
        val l = baseStr.length - 1
        repeat(len) { sb.append(baseStr[Random.nextInt(l)]) }
        return sb.toString()
    }

    private class Response(val aweme_detail: Detail) {
        class Detail(val desc: String, val video: Video)
        class Video(val play_addr: PlayAddr)
        class PlayAddr(val url_list: List<String>)
    }

    private class Params(
        val videoId: String,
        val msToken: String,
        val aid: Int = 6383,
        val versionCode: Int = 190500, val versionName: String = "19.5.0",
        val devicePlatform: String = "android",
        val osVersion: Int = 6,
        val updateVersionCode: String = "1704000",
        val pcClientType: Int = 1,
        var aBogus: String? = null
    ) {
        fun toAbogus(): String {
            return "aweme_id=$videoId&aid=$aid&version_code=$versionCode&version_name=$versionName&device_platform=$devicePlatform&os_version=$osVersion&update_version_code=$updateVersionCode&pc_client_type=$pcClientType&msToken=$msToken"
        }

        fun toUrl(): String {
            return "${toAbogus()}&a_bogus=${aBogus}"
        }
    }

    private class TTWid(
        val region: String = "cn",
        val aid: Int = 1768,
        val needFid: Boolean = false,
        val service: String = "www.ixigua.com",
        val migrate_info: MigrateInfo = MigrateInfo(),
        val cbUrlProtocol: Boolean = true,
        val union: Boolean = true
    ) {

        private class MigrateInfo(val ticket: String = "", val source: String = "node")
    }
}