package com.munch1182.android.net

import com.google.gson.Gson
import com.munch1182.android.lib.helper.FileHelper
import okhttp3.Cache
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor

val gson = Gson()
private val httpClient: OkHttpClient.Builder
    get() = OkHttpClient.Builder().addInterceptor(HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }).addInterceptor(CacheByQueryInterceptor())

fun newHttpClientBuilder(cache: Long = 50L * 1024L * 1024L) = httpClient.cache(
    Cache(FileHelper.newCache("net_cache"), cache)
)

suspend fun WebSocketService.connect(url: String) = connect(Request.Builder().url(url).build())

fun OkHttpClient.get(url: String, headers: Array<String> = arrayOf()): Response {
    val header = if (headers.isNotEmpty()) Headers.headersOf(*headers) else Headers.headersOf()
    val request = Request.Builder().url(url).headers(header).build()
    return try {
        newCall(request).execute()
    } catch (e: Exception) {
        e.printStackTrace()
        Response.Builder().code(901)
            .request(request)
            .message(e.message ?: "call error")
            .protocol(Protocol.HTTP_1_0)
            .build()
    }
}

inline fun <reified T> Response.resp(): T? {
    return try {
        if (!isSuccessful) return null
        val str = body.string()
        gson.fromJson(str, T::class.java)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}