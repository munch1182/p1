package com.munch1182.test.bluetooth

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
import com.ramcosta.composedestinations.generated.bluetooth.navgraphs.FeatureBluetoothNavGraph
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
annotation class TestBluetoothGraph {
    @ExternalNavGraph<FeatureBluetoothNavGraph>(start = true)
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
                navGraph = NavGraphs.testBluetooth,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(innerPadding)
            )
        }
    }
}
