import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task

private fun String.capitalize() = replaceFirstChar { it.uppercase() }

/**
 * 为指定flavors创建构建任务，并复制构建产物到指定目录; 可以执行多个构建
 *
 * 需要在task中使用；
 *
 * example:
 * ```kotlin
 * tasks.register("buildPublish111Flavors") {
 *     group = "build"
 *     description = "Build specified product flavors in release mode"
 *
 *     // 定义要构建的flavors
 *     val flavors = listOf("zkeg" to "apk", "core" to "aab")
 *
 *     build2Dir(flavors, "build/apk")
 * }
 * ```
 *
 * @param flavors 要构建的flavors，格式为`flavorName` to `buildType`，`buildType`为`apk`或`aab`
 * @param outputDir 构建产物的输出目录
 */
fun Task.build2Dir(flavors: List<Pair<String, String>>, outputDir: String?, clearDirIfBuild: Boolean = false) {
    if (flavors.isEmpty()) return
    println("🚀 配置构建任务")
    println("📋 Flavors: ${flavors.joinToString()}")
    println("🏗️ 构建类型: release")
    println("📁 输出目录: $outputDir")

    // 为每个flavor创建构建任务依赖
    flavors.forEach { (flavor, buildType) ->
        val taskName = when (buildType) {
            "aab" -> "bundle${flavor.capitalize()}Release"
            "apk" -> "assemble${flavor.capitalize()}Release"
            else -> throw GradleException("不支持的构建类型: $buildType")
        }
        dependsOn(taskName)
    }

    doLast {
        if (outputDir != null) {
            println("🚀 开始复制输出文件")

            // 清理并创建输出目录
            val targetDir = project.file(outputDir)
            if (clearDirIfBuild && targetDir.exists()) targetDir.deleteRecursively()
            targetDir.mkdirs()

            // 复制文件
            flavors.forEach { (flavor, buildType) ->
                when (buildType) {
                    "apk" -> project.copyApk(flavor, outputDir)
                    "aab" -> project.copyAab(flavor, outputDir)
                }
            }

            println("✅ 所有文件复制完成")
        }
    }
}

private fun Project.copyApk(flavor: String, outputDir: String) {
    val apkDir = file("build/outputs/apk/$flavor/release")
    if (!apkDir.exists()) {
        println("⚠️  APK目录不存在: $apkDir")
        return
    }

    val apkFiles = apkDir.listFiles { file -> file.isFile && file.extension == "apk" }
    val latestApk = apkFiles?.maxByOrNull { it.lastModified() }

    latestApk?.let { apk ->
        val targetFile = file("$outputDir/${apk.nameWithoutExtension}.apk")
        apk.copyTo(targetFile, overwrite = true)
        println("📋 复制APK: ${apk.name}")
    } ?: println("⚠️  未找到APK文件: $flavor")
}

private fun Project.copyAab(flavor: String, outputDir: String) {
    val bundleDir = file("build/outputs/bundle/${flavor}Release")
    if (!bundleDir.exists()) {
        println("⚠️  AAB目录不存在: $bundleDir")
        return
    }

    val aabFiles = bundleDir.listFiles { file -> file.isFile && file.extension == "aab" }
    val latestAab = aabFiles?.maxByOrNull { it.lastModified() }

    latestAab?.let { aab ->
        val targetFile = file("$outputDir/${aab.nameWithoutExtension}.aab")
        aab.copyTo(targetFile, overwrite = true)
        println("📦 复制AAB: ${aab.name}")
    } ?: println("⚠️  未找到AAB文件: $flavor")
}