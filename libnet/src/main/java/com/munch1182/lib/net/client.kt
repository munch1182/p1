package com.munch1182.lib.net

import com.google.gson.Gson
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor

val gson = Gson()
val httpClient: OkHttpClient.Builder
    get() = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })

suspend fun WebSocketService.connect(url: String) = connect(Request.Builder().url(url).build())

fun get(url: String, headers: Array<String> = arrayOf(), client: OkHttpClient = httpClient.build()): Response {
    val header = if (headers.isNotEmpty()) Headers.headersOf(*headers) else Headers.headersOf()
    val request = Request.Builder().url(url).headers(header).build()
    return client.newCall(request).execute()
}

inline fun <reified T> Response.convert(): T? {
    return try {
        if (!isSuccessful) return null
        val str = body.string()
        gson.fromJson(str, T::class.java)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}