package com.munch1182.p1.views

import androidx.compose.runtime.Composable
import com.munch1182.lib.helper.TextSegmenter
import com.munch1182.p1.ui.ClickButton

@Composable
fun TestView() {
    ClickButton("test1") { text1() }
}

private fun text1() {
    maina()
}


/**
 * 使用示例
 */
fun maina() {
    println("=== 多语言文本分段器示例 ===\n")

    val text = TextSegmenter()

    val str = listOf(
        "Hello, I am the cognitive",
        "Hello, I am the cognitive intelligence model developed by iFlytek. My name is iFlytek Spark Cognitive Model.",
        "Hello, I am the cognitive intelligence model developed by iFlytek. My name is iFlytek Spark Cognitive Model. I can communicate naturally with ",
        "Hello, I am the cognitive intelligence model developed by iFlytek. My name is iFlytek Spark Cognitive Model. I can communicate naturally with humans, answer questions, a",
    )
    str.forEach {
        text.processFullText(it).forEachIndexed { i, s ->
            println("第${i + 1}段：${s}")
        }
    }
    val flush = text.flushIncremental()
    println("flush: ${flush}")
}
