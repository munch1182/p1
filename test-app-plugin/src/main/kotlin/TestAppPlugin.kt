import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.getByType

abstract class TestAppExtension {
    abstract val feature: Property<String>
    abstract val label: Property<String>
}

class TestAppPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val ext = project.extensions.create("testApp", TestAppExtension::class.java)

        project.plugins.apply("com.munch1182.android.commonbuild_app")
        project.plugins.apply("org.jetbrains.kotlin.plugin.compose")
        project.plugins.apply("com.google.devtools.ksp")
        project.plugins.apply("com.google.dagger.hilt.android")

        val andr = project.extensions.getByType<ApplicationExtension>()
        andr.sourceSets.named("main").configure {
            manifest.srcFile(project.file("build/generated/testapp/AndroidManifest.xml"))
        }

        val libs = project.extensions.getByType<VersionCatalogsExtension>().named("libs")
        fun VersionCatalog.lib(name: String) = findLibrary(name).get().get()

        project.dependencies.apply {
            add("implementation", project.dependencies.project(mapOf("path" to ":lib-common")))
            add("implementation", project.dependencies.project(mapOf("path" to ":lib-android")))
            add("implementation", project.dependencies.project(mapOf("path" to ":core-base")))
            add("implementation", project.dependencies.project(mapOf("path" to ":core-ui")))
            add("implementation", ext.feature.map { f -> project.dependencies.project(mapOf("path" to ":feature-$f")) })

            add("implementation", platform(libs.lib("androidx-compose-bom")))
            add("implementation", libs.lib("androidx-compose-ui"))
            add("implementation", libs.lib("androidx-compose-ui-graphics"))
            add("implementation", libs.lib("androidx-compose-material3"))
            add("implementation", libs.lib("androidx-compose-material-icons"))
            add("implementation", libs.lib("androidx-activity-compose"))
            add("implementation", libs.lib("androidx-appcompat"))
            add("implementation", libs.lib("androidx-core-ktx"))
            add("implementation", libs.lib("androidx-fragment-ktx"))

            add("implementation", libs.lib("hilt-android"))
            add("implementation", libs.lib("hilt-navigation-compose"))
            add("implementation", libs.lib("compose-destinations"))
            add("ksp", libs.lib("hilt-ksp"))
            add("ksp", libs.lib("kotlin-metadata-jvm"))
            add("ksp", libs.lib("compose-destinations-ksp"))
        }

        val genTask = project.tasks.register("generateTestAppSources", GenerateTestAppTask::class.java) {
            this.feature.set(ext.feature)
            this.label.set(ext.label)
            outputDir.set(project.layout.buildDirectory.dir("generated/testapp"))
        }

        project.tasks.named("preBuild") {
            dependsOn(genTask)
        }

        val androidComponents = project.extensions.getByType<ApplicationAndroidComponentsExtension>()
        androidComponents.onVariants { variant ->
            variant.sources.java?.addGeneratedSourceDirectory(
                genTask,
                GenerateTestAppTask::outputDir
            )
        }
    }
}
