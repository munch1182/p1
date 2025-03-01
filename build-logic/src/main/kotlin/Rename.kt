import com.android.build.gradle.AbstractAppExtension
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * rename apk file
 */
fun AbstractAppExtension.setAPkRename(onlyRelease: Boolean = true) {
    applicationVariants.all {
        if (onlyRelease && buildType.isDebuggable) {
            return@all
        }
        var appName = applicationId.split(".").let {
            if (it.size >= 3) {
                it.slice(2 until it.size).joinToString(".")
            } else {
                applicationId
            }
        }
        if (buildType.applicationIdSuffix != null) {
            appName += buildType.applicationIdSuffix
        }
        val flavorName = productFlavors.joinToString("_") { it.name }
        if (flavorName.isNotEmpty()) {
            appName = "${appName}_${flavorName}"
        }
        val buildType = buildType.name
        val vName = versionName ?: "unknown"
        val vCode = versionCode
        val timestamp = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())
        // 使用了一个impl类，可能会被更改
        outputs.filterIsInstance<com.android.build.gradle.internal.api.ApkVariantOutputImpl>()
            .forEach {
                it.outputFileName =
                    "${appName}_${buildType}_${vName}_${vCode}_${timestamp}.apk"
            }
    }

}
