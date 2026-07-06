import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class GenerateTestAppTask : DefaultTask() {
    @get:Input
    abstract val feature: Property<String>

    @get:Input
    abstract val label: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val f = feature.get()
        val camel = f.replaceFirstChar { it.uppercaseChar() }
        val dir = outputDir.get().asFile
        dir.mkdirs()

        generateAppKt(dir, f, camel)
        generateManifest(dir, label.get())
    }

    private fun generateAppKt(dir: File, feature: String, camel: String) {
        val file = File(dir, "com/munch1182/test/$feature/App.kt")
        file.parentFile.mkdirs()
        file.writeText("""
package com.munch1182.test.$feature

import android.app.Application
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.fragment.app.FragmentActivity
import com.munch1182.core.base.CoreInit
import com.munch1182.core.ui.theme.P1Theme
import com.munch1182.lib.android.AppHelper
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.annotation.ExternalNavGraph
import com.ramcosta.composedestinations.annotation.NavHostGraph
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.generated.$feature.navgraphs.Feature${camel}NavGraph
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TestApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CoreInit.init(AppHelper)
    }
}

@NavHostGraph
annotation class Test${camel}Graph {
    @ExternalNavGraph<Feature${camel}NavGraph>(start = true)
    companion object Includes
}

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent(content = ::AppNavigation)
    }
}

@Composable
fun AppNavigation() {
    P1Theme {
        Scaffold(modifier = Modifier.fillMaxSize().background(Color.White)) { innerPadding ->
            DestinationsNavHost(
                navGraph = NavGraphs.test$camel,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(innerPadding)
            )
        }
    }
}
        """.trimIndent().trim())
    }

    private fun generateManifest(dir: File, label: String) {
        val file = File(dir, "AndroidManifest.xml")
        file.writeText("""
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <application
        android:name=".TestApp"
        android:allowBackup="true"
        android:label="$label"
        android:supportsRtl="true"
        android:theme="@style/Theme.AppCompat.NoActionBar"
        tools:ignore="MissingApplicationIcon">

        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
        """.trimIndent().trim())
    }
}
