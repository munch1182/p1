package com.munch1182.p1

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.iflytek.sparkchain.core.LLM
import com.iflytek.sparkchain.core.LLMCallbacks
import com.iflytek.sparkchain.core.LLMConfig
import com.iflytek.sparkchain.core.LLMError
import com.iflytek.sparkchain.core.LLMEvent
import com.iflytek.sparkchain.core.LLMFactory
import com.iflytek.sparkchain.core.LLMResult
import com.iflytek.sparkchain.core.SparkChain
import com.iflytek.sparkchain.core.SparkChainConfig
import com.munch1182.lib.AppHelper
import com.munch1182.lib.base.Loglog
import com.munch1182.lib.base.appDetailsPage
import com.munch1182.lib.result.isAllGrant
import com.munch1182.lib.result.permission
import com.munch1182.p1.ui.ButtonDefault
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser

class XFSparkchainActivity : AppCompatActivity() {

    private val vm by viewModels<VM>()
    private lateinit var llm: LLM

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentNoContainer { p -> XYSparkView(p) }
        initSpark()
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Composable
    fun XYSparkView(modifier: Modifier = Modifier) {
        val txt by vm.resMsg.observeAsState("")
        val content by vm.content.observeAsState(arrayOf())
        val showType by vm.showType.observeAsState(Show.HtmlText)
        val descriptor by remember { mutableStateOf(GFMFlavourDescriptor()) }

        LazyColumn(modifier = modifier.padding(16.dp)) {
            itemsIndexed(content) { index, item ->
                when (index) {
                    0 -> BTN("生成一张佩奇小头像。")
                    1 -> BTN("自我介绍模版，两句话以内。")
                    2 -> BTN("英文介绍短语，两句话以内。")
                    3 -> BTN("来两句古诗，不要解释。")
                    4 -> Row(verticalAlignment = Alignment.CenterVertically) {
                        ButtonDefault("当前显示: $showType") { vm.check() }
                    }

                    5 -> Text(txt)
                    else -> when (showType) {
                        Show.WebView -> {
                            AndroidView(factory = { context ->
                                WebView(context).apply {
                                    settings.apply {
                                        javaScriptEnabled = true
                                        domStorageEnabled = true
                                        cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                                    }
                                    setBackgroundColor(Color.Transparent.toArgb())
                                }
                            }, update = {
                                val node = MarkdownParser(descriptor).buildMarkdownTreeFromString(item)
                                val html = HtmlGenerator(item, node, descriptor).generateHtml()
                                it.loadData(html, "text/html", "UTF-8")
                            })
                        }

                        Show.Origin -> Text(item)
                        Show.HtmlText -> {
                            val node = MarkdownParser(descriptor).buildMarkdownTreeFromString(item)
                            val html = HtmlGenerator(item, node, descriptor).generateHtml()
                            Text(HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT).toString())
                        }
                    }
                }

            }
        }
    }

    @Composable
    fun BTN(text: String, modifier: Modifier = Modifier) {
        ButtonDefault(text, modifier) {
            permissionCheck {
                vm.start()
                llm.arun(text)
            }
        }
    }

    private fun permissionCheck(any: () -> Unit) {
        permission {
            arrayOf(READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE)
        }.intentIfDeniedForever(appDetailsPage).request {
            val allGrant = it.isAllGrant()
            vm.updateMsg(if (allGrant) "权限请求成功" else it.toString())
            if (allGrant) any()
        }
    }

    private fun initSpark() {
        val c = SparkChainConfig.builder().appID("111").apiKey("111").apiSecret("111")
        val code = SparkChain.getInst().init(AppHelper, c)
        if (code != 0) vm.updateMsg("初始化返回值: $code")
        llm = LLMFactory.textGeneration(LLMConfig.builder().domain("generalv3.5"))
        llm.registerLLMCallbacks(object : LLMCallbacks {
            override fun onLLMResult(p0: LLMResult?, p1: Any?) {
                //Loglog.log("content: ${p0?.content}")
                //Loglog.log("raw: ${p0?.raw}")
                p0?.content?.let { vm.updateContent(it) }
            }

            override fun onLLMEvent(p0: LLMEvent?, p1: Any?) {
                Loglog.log("event: $p0")
            }

            override fun onLLMError(p0: LLMError?, p1: Any?) {
                Loglog.log("err:$p0")
                p0?.apply {
                    vm.updateMsg("$errCode: $errMsg")
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        SparkChain.getInst().unInit()
    }

    sealed class Show {
        data object Origin : Show()
        data object HtmlText : Show()
        data object WebView : Show()
    }

    class VM : ViewModel() {

        private var sb = StringBuilder()
        private val sbs = MutableList(6) { "" }
        private var _resMsg = MutableLiveData("")
        val resMsg: LiveData<String> = _resMsg
        private var _content = MutableLiveData(sbs)
        val content: LiveData<Array<String>> = _content.map { it.toTypedArray() }
        private var _showType = MutableLiveData<Show>(Show.HtmlText)
        val showType: LiveData<Show> = _showType

        fun updateMsg(msg: String) {
            _resMsg.postValue(msg)
        }

        fun check() {
            val newType = when (_showType.value!!) {
                Show.HtmlText -> Show.WebView
                Show.Origin -> Show.HtmlText
                Show.WebView -> Show.Origin
            }
            _showType.postValue(newType)
        }

        fun updateContent(it: String) {
            sb.append(it)
            if (sbs.lastIndex >= 0) {
                sbs[sbs.lastIndex] = sb.toString()
                _content.postValue(sbs)
            }
        }

        fun start() {
            sb = StringBuilder()
            sbs.add(sb.toString())
            _content.postValue(sbs)
        }
    }
}