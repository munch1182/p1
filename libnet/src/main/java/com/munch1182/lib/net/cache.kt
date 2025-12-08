package com.munch1182.lib.net

import okhttp3.Interceptor
import okhttp3.Response

class CacheByQueryInterceptor : Interceptor {

    companion object {
        const val CACHE_QUERY = "cache_local"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url
        val cache = url.queryParameterValues(CACHE_QUERY)
        val newUrl = url.newBuilder()
            .addQueryParameter("cache", "true")
            .build()
        return chain.proceed(request.newBuilder().url(newUrl).build())
    }
}