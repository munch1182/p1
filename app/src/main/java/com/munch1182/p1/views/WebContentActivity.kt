package com.munch1182.p1.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import android.window.OnBackInvokedDispatcher
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.munch1182.lib.base.Loglog
import com.munch1182.p1.base.BaseActivity
import com.munch1182.p1.ui.setContentWithTheme
import com.munch1182.p1.ui.theme.PagePadding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class WebContentActivity : BaseActivity() {

    companion object {
        private const val KEY_URL = "url"
        fun url(ctx: Context, url: String? = null): Intent {
            return Intent(ctx, WebContentActivity::class.java).putExtra(KEY_URL, url)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentWithTheme { View(it) }
    }

    override fun getOnBackInvokedDispatcher(): OnBackInvokedDispatcher {
        setResult(RESULT_OK, Intent().apply { putExtra("result", "ok") })
        return super.getOnBackInvokedDispatcher()
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Composable
    private fun View(pd: PaddingValues) {
        var url by remember { mutableStateOf<String?>(null) }
        LaunchedEffect(Unit) {
            launch(Dispatchers.IO) {
                delay(200)
                url = ""
                delay(30)
                url = intent.getStringExtra(KEY_URL) ?: "https://www.baidu.com"
            }
        }
        if (url == null) {
            Text(
                "loading...", modifier = Modifier
                    .padding(pd)
                    .padding(PagePadding)
            )
        } else {
            AndroidView(factory = { context ->
                WebView(context).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                    }
                    loadData("<html><body>loading...</body></html>", "text/html", "utf-8")
                }
            }, modifier = Modifier.padding(pd), update = {
                Loglog.log("${1111}")
                if (url.isNullOrBlank().not()) it.loadUrl(url!!)

            })
        }
    }
}