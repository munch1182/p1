import com.android.build.gradle.internal.dsl.SigningConfig
import org.gradle.api.Project
import java.io.File
import java.util.Properties

object AppVersion {
    const val COMPILE_SDK = 35
    const val MIN_SDK = 24
    const val TARGET_SDK = 35

    // 所以所有对外版本必须提交后交付
    fun versionCode() = GitVersion.commitCount()?.toIntOrNull() ?: 0

    // 会直接截取-，如果tag中有连接符，至少使用_
    fun versionName() = GitVersion.latestCommitTag()?.split('-')?.firstOrNull() ?: "0.1.0"
}


private fun find(file: File): Properties? {
    if (file.exists()) {
        return Properties().apply { load(java.io.FileInputStream(file)) }
    }
    return null
}

fun Project.sign(file: File, sc: SigningConfig) {
    kotlin.runCatching {
        val prop = find(file) ?: return;
        sc.storeFile(file(prop.getProperty("keyFile") ?: return))
        sc.storePassword(prop.getProperty("storePassword"))
        sc.keyAlias(prop.getProperty("keyAlias"))
        sc.keyPassword(prop.getProperty("keyPassword"))
    }
}