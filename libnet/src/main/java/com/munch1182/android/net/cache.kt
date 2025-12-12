package com.munch1182.android.net

import com.munch1182.android.lib.base.log
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.TimeUnit

fun String.withCache(cache: Int) = (if (this.contains("?")) '&' else '?').let {
    "${this}${it}${CacheByQueryInterceptor.CACHE_QUERY}=$cache"
}

class CacheByQueryInterceptor : Interceptor {
    private val log = log()

    companion object {
        const val CACHE_QUERY = "cache_local"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url
        val cache = url.queryParameter(CACHE_QUERY)
        val newUrl = url.newBuilder().removeAllQueryParameters(CACHE_QUERY).build()
        val newRequest = request.newBuilder().url(newUrl)
        return if (cache?.isNotEmpty() == true && (cache.toIntOrNull() ?: 0) > 0) {
            val maxAgeSec = cache.toInt()

            val modifiedRequest = newRequest.cacheControl(
                CacheControl.Builder().maxAge(maxAgeSec, TimeUnit.SECONDS).build()
            ).build()
            log.logStr("Use cache for $maxAgeSec seconds by query")
            val resp = chain.proceed(modifiedRequest)
            resp.newBuilder()
                .request(modifiedRequest)
                .removeHeader("Pragma")
                .removeHeader("Cache-Control")
                .header("Cache-Control", "public, max-age=$maxAgeSec")
                .build()
        } else {
            chain.proceed(newRequest.build())
        }
    }
}