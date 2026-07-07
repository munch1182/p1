/**
 * 获取基于 Git 提交历史计数的 Android Version Code。
 *
 * 计算逻辑：
 * - 使用 `git rev-list --count HEAD` 获取当前分支自仓库初始提交以来的总提交数。
 * - 只要主分支（如 main/release）历史保持线性增长（不进行 force push 或 reset），
 *   该值严格单调递增，完全满足 Google Play 对 versionCode 的要求。
 *
 * 优点：
 * - 无需依赖 Git Tag，从根本上避免了解析错误和版本回退风险。
 * - 天然单调递增，无溢出担忧（21 亿上限在实际项目中几乎不可能达到）。
 * - 实现极简，健壮可靠。
 *
 * 注意事项：
 * - 如果在开发分支上执行了破坏性历史改写（git reset/rebase），仅会影响该分支上的
 *   测试版构建，正式发布分支不受影响，因此对正式渠道完全安全。
 *
 * @return 基于提交计数的整型版本代码。
 */
fun getGitVersionCode(): Int {
    val count = runCommand("git rev-list --count HEAD").toIntOrNull()
        ?: throw IllegalStateException("Unable to retrieve git commit count. Please check the repository.")
    return count
}

/**
 * 获取用于展示的版本名称，包含 debug / dirty 后缀。
 * 规则：
 * - 如果 HEAD 恰好指向一个 Tag（exact match）且工作区干净 → 原样返回（release）
 * - 如果工作区存在未提交修改 → 追加 "-dirty"
 * - 如果工作区不存在未提交修改 HEAD 且 不是 exact tag → 追加 "-dev"
 *
 */
fun getVersionNameForDisplay(): String {
    val rawTag = getRawTagName()  // 基础 Tag，如 "v1.2.3"

    val isExactTag = runCatching {
        runCommand("git describe --exact-match --tags HEAD")
    }.getOrNull().isNullOrBlank().not()

    val isDirty = runCatching {
        runCommand("git status --porcelain")
    }.getOrNull()?.isNotBlank() == true

    val suffix = buildString {
        if (isDirty) append("-dirty") else if (!isExactTag) append("-dev")

    }

    return rawTag + suffix
}

/**
 * 供内部计算使用的原始 Tag（最近一个 tag 名，不含后缀）。
 */
private fun getRawTagName(): String {
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
    val process = ProcessBuilder(parts).redirectErrorStream(true).start()

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