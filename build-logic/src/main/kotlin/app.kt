import com.android.build.gradle.internal.dsl.SigningConfig
import org.gradle.api.Project
import java.io.File
import java.util.Properties

object AppVersion {
    const val COMPILE_SDK = 36
    const val MIN_SDK = 26
    const val TARGET_SDK = 36

    /**
     * git所有提交次数作为versionCode
     */
    fun versionCode() = GitVersion.commitCount()?.toIntOrNull() ?: 0

    /**
     * 将tag+tag之后提交的次数作为versionName；
     *
     * 如tag为1.0.0，提交了3次，则versionName为1.0.3
     *
     * 如果tag是3位，则会直接替换掉第三位；如果是其它类型的tag，则会返回"0.0.1"
     */
    fun versionName() = GitVersion.latestCommitTag()?.tagToVersionName() ?: "0.0.1"
}

fun String.tagToVersionName(): String? {
    // 使用正则表达式匹配版本格式，并捕获可选的dirty后缀
    val pattern = """^([^-]+)-(\d+)-g[a-f0-9]+(-.+)?$""".toRegex()
    val matchResult = pattern.find(this) ?: return null // 如果匹配失败，返回原字符串

    val (tag, commitCount, dirtySuffix) = matchResult.destructured

    // 处理标签部分
    val tagParts = tag.split('.')
    val versionParts = mutableListOf<String>()

    // 确保至少有3部分，不足则补"0"
    for (i in 0 until 3) {
        versionParts.add(
            when {
                i < tagParts.size -> if (i == 2) commitCount else tagParts[i] // 第三位替换为提交次数
                else -> "0" // 不足的部分补0
            }
        )
    }

    // 组合结果并保留dirty后缀
    return versionParts.joinToString(".") + dirtySuffix
}

private fun find(file: File): Properties? {
    if (file.exists()) {
        return Properties().apply { load(java.io.FileInputStream(file)) }
    }
    return null
}

fun Project.sign(file: File, sc: SigningConfig) {
    kotlin.runCatching {
        val prop = find(file) ?: return
        sc.storeFile(file(prop.getProperty("keyFile") ?: return))
        sc.storePassword(prop.getProperty("storePassword"))
        sc.keyAlias(prop.getProperty("keyAlias"))
        sc.keyPassword(prop.getProperty("keyPassword"))
    }
}