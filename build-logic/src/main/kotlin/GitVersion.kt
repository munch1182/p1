/**
 * 获取基于语义化标签（Tag）和提交偏移（Offset）的 Android Version Code。
 *
 * 计算逻辑：
 * 1. 提取标签中的数字部分（支持 "v1.2.3"、"1.2.3" 等格式）。
 * 2. 采用权重累加：Major * 1,000,000 + Minor * 1,000 + Patch。
 * 3. 加上自该标签起的提交次数（Offset）。
 *
 * 理论极限与风险说明：
 * - 字段溢出风险：本算法假设 Minor 和 Patch 均不超过 999。若 Minor 为 1000，则会向 Major 进位（如 1.1000.0 变成 2.000.0）。
 * - Offset 挤压风险：若两次 Tag 之间的提交数超过 1000，则会向 Patch 进位。
 * - 整型溢出风险：Android versionCode 为 32 位正整数，最大值为 2,147,483,647。
 *   按本算法，Major 最大支持 2147（此时 Minor/Patch/Offset 必须为 0），超过此值将导致构建失败。
 *
 * @return 转换后的整型版本代码。
 */
fun getGitVersionCode(): Int {
    val tagName = getGitTagName()

    // 使用正则提取所有数字部分，例如 "v1.22.5" -> ["1", "22", "5"]
    val digits = Regex("\\d+").findAll(tagName)
        .map { it.value.toInt() }
        .toList()

    val major = digits.getOrElse(0) { 0 }
    val minor = digits.getOrElse(1) { 0 }
    val patch = digits.getOrElse(2) { 0 }

    // 计算标签基础权重（每位预留 1000 个位置）
    val base = (major * 1_000_000) + (minor * 1_000) + patch

    // 获取自该标签以来的提交次数（即偏移量）
    val offset = runCommand("git rev-list --count $tagName..HEAD").toIntOrNull() ?: 0

    return base + offset
}

/**
 * 获取 Git 仓库最近的一个标签名称。
 *
 * 通过 `git describe --tags --abbrev=0` 查找。
 *
 * @return 最近的标签字符串（如 "v1.0.1" 或 "1.0.1"）。
 * @throws IllegalStateException 如果未发现任何标签，则中止构建并提示。
 */
fun getGitTagName(): String {
    val tag = runCommand("git describe --tags --abbrev=0")
    if (tag.isEmpty()) {
        throw IllegalStateException(
            """Git TAG not found. 
            Please ensure you are in a git repository and have at least one tag.""".trimMargin()
        )
    }
    return tag
}

private fun runCommand(command: String, timeoutSeconds: Long = 10): String {
    val parts = command.split("\\s".toRegex()) // 仍建议用健壮解析，此处略
    val process = ProcessBuilder(parts)
        .redirectErrorStream(true)
        .start()

    val finished = process.waitFor(timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS)
    if (!finished) {
        process.destroyForcibly()
        throw RuntimeException("Command timed out after ${timeoutSeconds}s: $command")
    }

    val output = process.inputStream.bufferedReader().use { it.readText().trim() }
    if (process.exitValue() != 0) {
        throw RuntimeException("Command failed with exit code ${process.exitValue()}: $command\nOutput: $output")
    }
    return output
}