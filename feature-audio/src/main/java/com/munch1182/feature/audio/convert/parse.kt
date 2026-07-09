package com.munch1182.feature.audio.convert

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * 解析 JSON 字符串为 [ToolConfig]。
 * 注意：解析失败会抛出异常。
 */
fun parse(str: String): ToolConfig = Json.decodeFromString(str)

/**
 * 根据工具配置、格式命令和输入文件构建完整命令。
 * @param format 当前选中的格式
 * @param formatCmd 已替换参数后的编码命令（如 "-c:a libmp3lame -ar 16000 ..."）
 * @param fromFile 源文件（可为空，为空时使用占位符）
 * @return 完整命令（含 from/to 替换）
 */
fun ToolConfig.buildCommand(format: ConvertFormat, formatCmd: String, fromFile: File?): String {
    val from = fromFile?.absolutePath ?: "*"
    val to = if (fromFile != null) {
        val parent = fromFile.parentFile
        val nameWithoutExt = fromFile.nameWithoutExtension
        File(parent, "${nameWithoutExt}_converted.${format.id}").absolutePath
    } else {
        "*_${format.id}.*"
    }
    return cmd
        .replace("{from}", from)
        .replace("{to}", to)
        .replace("{format}", formatCmd)
}

private val DOUBLE_BRACE_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}")

/**
 * 将字符串中的 {{key}} 占位符替换为映射中的实际值。
 * 如果映射中缺少某个键，则抛出 IllegalArgumentException。
 *
 * @param values 占位符名称到值的映射
 * @return 替换后的字符串
 * @throws IllegalArgumentException 当存在未定义的占位符时
 */
fun String.replaceTemplate(values: Map<String, Option>): String {
    if (isEmpty()) return this

    val matcher = DOUBLE_BRACE_PATTERN.matcher(this)
    val result = StringBuffer()

    while (matcher.find()) {
        val key = matcher.group(1)
            ?: throw IllegalStateException("Unexpected empty placeholder group")
        val replacement = values[key]
            ?: throw IllegalArgumentException("Missing value for placeholder: {{$key}}")
        matcher.appendReplacement(result, Matcher.quoteReplacement(replacement.value))
    }
    matcher.appendTail(result)
    return result.toString()
}

/**
 * 工具配置，对应 JSON 中单个工具的配置。
 * @param cmd 命令模板，如 "ffmpeg -y {from} {format} {to}"
 * @param params 参数名 -> 可选值列表
 * @param formats 可选择的输出格式列表
 */
@Serializable
data class ToolConfig(
    val cmd: String,
    val params: Map<String, List<Option>>,
    val formats: List<ConvertFormat>
)

/**
 * 参数选项，用于展示下拉列表。
 * @param name 显示名称
 * @param value 实际参数值
 */
@Serializable
data class Option(
    val name: String,
    val value: String
)

/**
 * 输出格式配置。
 * @param id 格式标识（也用于生成输出文件扩展名）
 * @param name 显示名称
 * @param params 使用的参数名列表（需在 [ToolConfig.params] 中定义）
 * @param cmd 编码命令模板，使用 {{key}} 占位符引用参数值
 */
@Serializable
data class ConvertFormat(
    val id: String,
    val name: String,
    val params: List<String>,
    val cmd: String
)