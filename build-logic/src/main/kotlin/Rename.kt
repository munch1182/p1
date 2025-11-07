import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Project
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 重命名生成的apk/aab文件
 *
 * 同时重命名apk/aab，并会将Flavors的名称添加到文件名中；
 * 缺点是无法调整顺序
 */
fun Project.renameApkAab() {
    val android = extensions.getByType(ApplicationExtension::class.java)
    val name = android.defaultConfig.applicationId?.split(".")?.lastOrNull() ?: "app"
    val date = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val defName = "${name}-${android.defaultConfig.versionName}-$date"
    setProperty("archivesBaseName", defName)
}
