package com.munch1182.feature.audio.convert

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * 注意解析失败会抛出异常
 */
fun parse(str: String) = Json.decodeFromString<ConvertConfig>(str)

private val DOUBLE_BRACE_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}")

/**
 * 将字符串中的 {{key}} 占位符替换为映射中的实际值。
 * 如果映射中缺少某个键，则抛出 IllegalArgumentException。
 *
 * @param values 占位符名称到值的映射
 * @return 替换后的字符串
 * @throws IllegalArgumentException 当存在未定义的占位符时
 */
fun String.replaceTemplate(values: Map<String, String?>): String {
    if (isEmpty()) return this  // 空串无需处理

    val matcher = DOUBLE_BRACE_PATTERN.matcher(this)
    val result = StringBuffer()

    while (matcher.find()) {
        // 获取占位符内的键名（正则保证至少一个非}字符，因此 group(1) 不会为 null）
        val key = matcher.group(1)
            ?: throw IllegalStateException("Unexpected empty placeholder group")
        // 从映射中取值，如果为 null 则抛出异常
        val replacement = values[key]
            ?: throw IllegalArgumentException("Missing value for placeholder: {{$key}}")
        // 安全替换（转义 $ 和 \）
        matcher.appendReplacement(result, Matcher.quoteReplacement(replacement))
    }
    matcher.appendTail(result)
    return result.toString()
}

/**
 * @param params 参数, 固定参数名及其可选值, 比如: "bitrate": ["16000", "320000"]
 *
 */
@Serializable
data class ConvertConfig(
    val params: Map<String, List<String>>, val formats: List<ConvertFormat>
)

/**
 * 格式配置
 *
 * @param params 可选的参数名, 即[ConvertConfig.params]的key
 * @param cmd 命令模版, 使用{param}表示变量, 会从[ConvertConfig.params]中去获取值并替换
 */
@Serializable
data class ConvertFormat(
    val id: String, val name: String, val params: List<String>, val cmd: String
)