package com.munch1182.feature.browser

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.viewbinding.ViewBinding
import com.munch1182.core.base.BaseActivity
import com.munch1182.core.ui.theme.paddingPage
import com.munch1182.feature.browser.databinding.ActivityBrowserBinding

private inline fun <reified T : ViewBinding> Activity.bind(crossinline inflate: (LayoutInflater) -> T): Lazy<T> {
    return lazy {
        val vb = inflate(layoutInflater)
        setContentView(vb.root)
        vb
    }
}

class BrowserActivity : BaseActivity() {

    private val bind by bind(ActivityBrowserBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initTitleBar(bind.title)
        loadView()
    }

    private fun initTitleBar(titleBar: ComposeView) {
        titleBar.setContent {
            Row(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(Modifier.paddingPage()) {
                    Text("正在使用", style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun loadView() {
        bind.webview.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            loadUrl("https://www.doubao.com/chat/")
        }
    }
}

