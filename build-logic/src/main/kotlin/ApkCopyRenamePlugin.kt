import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.ApplicationVariant
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Copy
import org.gradle.internal.extensions.stdlib.capitalized
import org.gradle.kotlin.dsl.findByType
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class ApkCopyRenamePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val extension = target.extensions.create("renameApk", RenameExtension::class.java)

        target.plugins.withId("com.android.application") {
            val androidComponents = target.extensions.findByType(ApplicationAndroidComponentsExtension::class) ?: return@withId
            androidComponents.onVariants { variant ->
                if (variant.buildType != "release") return@onVariants

                target.registerCopyTask(
                    variant,
                    variant.artifacts.get(SingleArtifact.APK),
                    "assemble${variant.name.capitalized()}",
                    extension
                )
                target.registerCopyTask(
                    variant,
                    variant.artifacts.get(SingleArtifact.BUNDLE),
                    "bundle${variant.name.capitalized()}",
                    extension
                )
            }
        }
    }

    private fun Project.registerCopyTask(
        variant: ApplicationVariant, from: Provider<out FileSystemLocation>, sourceTaskName: String, extension: RenameExtension
    ) {
        val copyTask = tasks.register("copy${variant.name.capitalized()}From${sourceTaskName.capitalized()}", Copy::class.java) {
            from(from)
            into(projectDir.resolve(extension.toDir.get()))
            rename { fileName ->
                val ext = fileName.split(".").lastOrNull()
                if (ext == "apk" || ext == "aab") {
                    val newName = extension.nameNoExt.get().getNameNoExt(variant, this@registerCopyTask)
                    if (newName != null) {
                        return@rename "$newName.$ext"
                    }
                }
                return@rename fileName
            }
            doLast {
                println("文件已重命名并复制到: ${extension.toDir.get()}")
            }
        }
        tasks.matching { it.name == sourceTaskName }.configureEach {
            finalizedBy(copyTask)
        }
    }
}

open class RenameExtension(project: Project) {
    val toDir: Property<File> = project.objects.property(File::class.java).convention(project.projectDir.resolve("./apk"))
    val nameNoExt: Property<ApkAabNameNoExtProvider> = project.objects.property(ApkAabNameNoExtProvider::class.java).convention(object : ApkAabNameNoExtProvider {
        override fun getNameNoExt(variant: ApplicationVariant, project: Project): String {
            val curr = SimpleDateFormat("yyyyMMddHHmmss", Locale.ROOT).format(System.currentTimeMillis())
            val type = variant.buildType ?: "unknown"
            val flavor = variant.flavorName ?: "main"
            val name = variant.applicationId.get().split(".").lastOrNull() ?: "app"

            val app = project.extensions.findByType(ApplicationExtension::class)
            val versionCode = app?.defaultConfig?.versionCode ?: 0
            val versionName = app?.defaultConfig?.versionName ?: "0.0.0"

            return "${name}_${versionName}_${versionCode}_${flavor}_${type}_$curr"
        }
    })
}

interface ApkAabNameNoExtProvider {
    fun getNameNoExt(variant: ApplicationVariant, project: Project): String?
}