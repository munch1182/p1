import com.android.build.api.dsl.SigningConfig
import org.gradle.api.Project
import java.io.File
import java.util.Properties

object AppConfig {
    const val COMPILE_SDK = 36
    const val MIN_SDK = 26

    fun versionCode() = getGitVersionCode()
    fun versionName() = getGitTagName()
}

fun loadSigningProperties(file: File): Properties? {
    if (!file.exists()) return null
    return Properties().apply { file.inputStream().use(::load) }
}

fun Project.sign(signingProps: Properties, signingConfig: SigningConfig) {
    signingConfig.storeFile = file(signingProps["storeFile"] as? String ?: error("Missing storeFile"))
    signingConfig.storePassword = signingProps["storePassword"] as? String ?: error("Missing storePassword")
    signingConfig.keyAlias = signingProps["keyAlias"] as? String ?: error("Missing keyAlias")
    signingConfig.keyPassword = signingProps["keyPassword"] as? String ?: error("Missing keyPassword")
}