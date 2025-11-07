import java.io.File
import java.util.concurrent.TimeUnit

internal object GitVersion {

    // 返回当前项目的提交次数，与分支无关
    internal fun commitCount() =
        kotlin.runCatching { "git rev-list --all --count HEAD".runCMD().trim() }.getOrNull()

    // 此指令只能在至少有一个tag的前提下使用，会返回
    // `<距离当前最近的tag>-<该tag后的提交次数>-g<最新提交的前7位HASH值>-<如果该版本未提交，则会添加后缀-debug>`
    // 的固定格式
    // 例如`0.1.0-3-gxxxxxxx-debug`表示tag0.1.0之后的第3次提交，且该版本未提交到git
    // 如果直接返回`0.1.0`则表示tag0.1.0之后无提交
    // 如果没有tag会直接报错
    internal fun latestCommitTag() =
        kotlin.runCatching { "git describe --tags --dirty=-debug".runCMD().trim() }.getOrNull()

}

private fun String.runCMD(
    file: File = File("."), timeoutAmount: Long = 15
) =
    ProcessBuilder(split("\\s(?=(?:[^'\"`]*(['\"`])[^'\"`]*\\1)*[^'\"`]*$)".toRegex()))
        .directory(
            file
        ).redirectOutput(ProcessBuilder.Redirect.PIPE)
        .redirectError(ProcessBuilder.Redirect.PIPE)
        .start()
        .apply { waitFor(timeoutAmount, TimeUnit.SECONDS) }
        .run {
            val error = errorStream.bufferedReader().readText().trim()
            if (error.isNotEmpty()) {
                throw Exception(error)
            }
            inputStream.bufferedReader().readText().trim()
        }