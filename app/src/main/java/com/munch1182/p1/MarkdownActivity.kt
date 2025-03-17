package com.munch1182.p1

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.text.HtmlCompat
import com.munch1182.lib.result.intent
import com.munch1182.p1.ui.ButtonDefault
import com.munch1182.p1.ui.theme.P1Theme
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser

class MarkdownActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentWithBase { Markdown() }
    }

    @Composable
    fun Markdown() {
        val ctx = LocalContext.current
        var markText by remember {
            mutableStateOf(
                "**标题** \r\n[baidu link](https://baidu.com)"
            )
        }
        var html by remember { mutableStateOf("") }
        var txt by remember { mutableStateOf("") }
        Text(markText)
        ButtonDefault("转换成html") {
            val descriptor = GFMFlavourDescriptor()
            val node = MarkdownParser(descriptor).buildMarkdownTreeFromString(markText)
            html = HtmlGenerator(markText, node, descriptor).generateHtml()
        }
        Text(html)
        if (html.isNotBlank()) {
            ButtonDefault("显示在webview中") {
                intent(WebActivity.showData(ctx, html)).request {}
            }
            ButtonDefault("将html转为text") {
                txt = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT).toString()
            }
            Text(txt)
        }

    }

    @Preview
    @Composable
    private fun MarkdownPreview() {
        P1Theme { Markdown() }
    }
}