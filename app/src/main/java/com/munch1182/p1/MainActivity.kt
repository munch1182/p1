package com.munch1182.p1

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.munch1182.core.base.BaseActivity
import com.munch1182.core.ui.PageAnimatedStyle
import com.munch1182.core.ui.theme.P1Theme
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.annotation.ExternalNavGraph
import com.ramcosta.composedestinations.annotation.NavHostGraph
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.generated.audio.navgraphs.FeatureAudioNavGraph
import com.ramcosta.composedestinations.generated.bluetooth.navgraphs.FeatureBluetoothNavGraph
import com.ramcosta.composedestinations.generated.browser.navgraphs.FeatureBrowserNavGraph
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent(content = ::AppNavigation)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) (application as? App)?.endTimer()
    }
}

/**
 * 定义整体的导航结构
 *
 * 1. 所有使用@Destination<AppGraph>注解的为一级;
 * 2. Includes中所有附件的导航视图
 */
@NavHostGraph
annotation class AppGraph {
    @ExternalNavGraph<FeatureBluetoothNavGraph>
    @ExternalNavGraph<FeatureAudioNavGraph>
    @ExternalNavGraph<FeatureBrowserNavGraph>
    companion object Includes
}

/**
 * 导航
 */
@Composable
fun AppNavigation(env: EnvViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { _, destination, arguments ->
            env.analytics.trackScreen(
                "OnDestinationChanged",
                mapOf(
                    "name" to (destination.route ?: "null"),
                    "arguments" to (arguments?.toString() ?: "null")
                )
            )
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose {
            navController.removeOnDestinationChangedListener(listener)
        }
    }
    P1Theme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            DestinationsNavHost(
                navGraph = NavGraphs.app, // NavGraphs是一个ksp生成的类
                navController = navController, modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(innerPadding), defaultTransitions = PageAnimatedStyle
            )
        }
    }
}
