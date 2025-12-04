import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.configure

/**
 * 在build.gradle.kts中注册
 *
 * 使用：
 * 直接引用插件即可(id定义于build.gradle.kts)：
 * ```kts
 * plugins {
 *     id("munch1182.publish-local")
 * }
 *
 * publishToMavenLocal { // extensions.create的name
 *     groupId = "com.munch1182"
 *     artifactId = "weight"
 *     version = "1.0.0"
 * }
 * ```
 *
 * 发布： Tasks/publishing/publish
 */
class PublishToLocalPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.plugins.apply("maven-publish")
        val extension = target.extensions.create(
            "publishToMavenLocal", PublishToLocalExtension::class.java, target
        )

        target.afterEvaluate {
            val sourcesJar = target.tasks.register("sourcesJar", Jar::class.java) {
                archiveClassifier.set("sources")
                val android = target.extensions.getByName("android") as LibraryExtension
                from(android.sourceSets.getByName("main").java.srcDirs)
            }
            target.extensions.configure<PublishingExtension> {
                publications {
                    create("release", MavenPublication::class.java) {
                        from(target.components.findByName("release"))

                        groupId = extension.groupId
                        artifactId = extension.artifactId
                        version = extension.version

                        artifact(sourcesJar)
                    }
                }
                repositories {
                    maven { url = project.uri(extension.localPath) }
                }
            }
        }
    }
}

open class PublishToLocalExtension(project: Project) {
    var groupId: String = project.group.toString()
    var artifactId: String = project.name
    var version: String = project.version.toString()
    var localPath: String = project.rootProject.file("maven-local").absolutePath
}
