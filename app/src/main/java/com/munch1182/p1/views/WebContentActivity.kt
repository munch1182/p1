package com.munch1182.p1.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import com.munch1182.p1.base.BaseActivity
import com.munch1182.p1.base.bind
import com.munch1182.p1.databinding.ActivityWebContentBinding

class WebContentActivity : BaseActivity() {

    companion object {
        private const val KEY_URL = "url"
        fun url(ctx: Context, url: String? = null): Intent {
            return Intent(ctx, WebContentActivity::class.java).putExtra(KEY_URL, url)
        }
    }

    private val bind by bind(ActivityWebContentBinding::inflate)

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind.webView.settings.apply { javaScriptEnabled = true }
        val url = intent.getStringExtra(KEY_URL)
        if (url != null) {
            bind.webView.loadUrl(url)
            bind.container.visibility = View.GONE
        } else {
            bind.btn.setOnClickListener {
                bind.webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        view?.evaluateJavascript("javascript:sign(12313,123123)") { res ->
                            bind.res.post { bind.res.text = res }
                        }
                    }
                }
                bind.webView.loadUrl("file:///android_asset/abogus.html")
            }
        }
    }


}