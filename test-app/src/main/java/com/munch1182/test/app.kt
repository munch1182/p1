package com.munch1182.test

import android.app.Application
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.fragment.app.FragmentActivity
import com.munch1182.core.base.CoreInit
import com.munch1182.core.ui.theme.P1Theme
import com.munch1182.core.ui.theme.paddingPage
import com.munch1182.lib.android.AppHelper
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TestApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CoreInit.init(AppHelper)
    }
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
        Scaffold(modifier = Modifier
            .fillMaxSize()
            .background(Color.White)) { innerPadding ->
            Column(Modifier
                .padding(innerPadding)
                .paddingPage()) {
                Text("test page")
            }
        }
    }
}
