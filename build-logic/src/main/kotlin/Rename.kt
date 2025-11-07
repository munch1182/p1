import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.ApplicationVariant
import org.gradle.api.Project
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Project.rename(onlyRelease: Boolean = true) {
    val androidComponents = extensions.getByType(ApplicationAndroidComponentsExtension::class.java)

    androidComponents.onVariants { variant ->
        if (onlyRelease && variant.buildType != "release") return@onVariants

        val baseFileName = generateBaseFileName(variant)

        variant.outputs.filterIsInstance<com.android.build.gradle.internal.api.ApkVariantOutputImpl>()
            .forEach { it.outputFileName = "${baseFileName}.apk" }
    }
}

private fun generateBaseFileName(variant: ApplicationVariant): String {
    var appName = variant.applicationId.get().split(".").let { parts ->
        if (parts.size >= 3) parts.slice(2 until parts.size).joinToString(".")
        else variant.applicationId.get()
    }

    val flavorNames = variant.flavorName
    if (!flavorNames.isNullOrEmpty()) {
        appName += "_${flavorNames}"
    }

    val buildType = variant.buildType
    val vName = variant.name
    val timestamp = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())

    return "${appName}_${buildType}_${vName}_${timestamp}"
}