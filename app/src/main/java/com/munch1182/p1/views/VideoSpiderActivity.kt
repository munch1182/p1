package com.munch1182.p1.views

import android.os.Bundle
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.munch1182.lib.helper.ClipboardHelper
import com.munch1182.p1.base.BaseActivity
import com.munch1182.p1.ui.ClickButton
import com.munch1182.p1.ui.Split
import com.munch1182.p1.ui.setContentWithRv
import kotlinx.coroutines.delay

class VideoSpiderActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentWithRv { Views() }
    }

    @Composable
    private fun Views() {
        var input by remember { mutableStateOf("") }
        var url by remember { mutableStateOf("") }
        var videoUrl by remember { mutableStateOf("") }
        TextField(input, {
            url = ""
            input = it
        }, maxLines = 20, minLines = 10, modifier = Modifier.fillMaxWidth())

        ClickButton("确定") {
            url = parse(input) ?: ""
            if (url.isNotEmpty()) videoUrl = parseVideoUrl(url) ?: ""
        }

        if (url.isNotEmpty()) {
            Split()
            Text("网址: $url")
            if (videoUrl.isNotEmpty()) Text("视频地址：$videoUrl")
        }

        LaunchedEffect(null) {
            delay(320L)
            input = ClipboardHelper.copyFrom2Str() ?: ""
        }
    }

    private fun parse(str: String): String? {
        return parseUrl(str).firstOrNull()
    }

    private fun parseVideoUrl(url: String): String? {
        return url
    }

    private fun parseUrl(text: String): List<String> {
        val regex = """\b((?:https?|ftp|file)://|www\.)[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|]""".toRegex()

        return regex.findAll(text).map { match ->
            // 为 www 开头的网址自动添加 https:// 协议
            if (match.value.startsWith("www.")) "https://${match.value}" else match.value
        }.filter { !it.contains(",") }.toList()
    }
}