package com.munch1182.p1

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import android.window.OnBackInvokedDispatcher
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class WebActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentNoContainer { Web(it) }
    }

    override fun getOnBackInvokedDispatcher(): OnBackInvokedDispatcher {
        setResult(RESULT_OK, Intent().apply { putExtra("result", "ok") })
        return super.getOnBackInvokedDispatcher()
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun Web(modifier: Modifier = Modifier) {
    var url by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        launch(Dispatchers.IO) {
            delay(300)
            url = "https://www.baidu.com"
        }
    }
    AndroidView(factory = { context ->
        WebView(context).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
            }
            loadData("<html><body>loading...</body></html>", "text/html", "utf-8")
        }
    }, modifier = modifier, update = { url?.apply { it.loadUrl(this) } })
}