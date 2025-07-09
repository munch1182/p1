package com.munch1182.p1

import org.junit.Test

class NetParseUnitTest {
    @Test
    fun test() {
        val text = """
        访问官网 http://example.com 或试试 ftp://files.example.com，
        也可浏览 www.example.com/path?query=value。注意邮箱：user@example.com。
        无效URL：http://invalid,domain.com/，有效URL：https://valid-domain.com/?id=123#section
        括号包裹的URL：(https://parenthesized.example)
        带结尾标点的URL：http://punctuation.com/end. 或者 https://another.com?query=value;
        https://a.com?query=value.
        【游客12312312123】 https://b23.tv/1wqb10FA1y
    """.trimIndent()
        val urls = parseUrl(text)
        urls.forEachIndexed { index, s -> println("${index}: [$s]") }
    }

    private fun parseUrl(text: String): List<String> {
        val regex = """\b((?:https?|ftp|file)://|www\.)[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|]""".toRegex()

        return regex.findAll(text).map { match ->
            // 为 www 开头的网址自动添加 https:// 协议
            if (match.value.startsWith("www.")) "https://${match.value}" else match.value
        }.filter { !it.contains(",") }.toList()
    }
}