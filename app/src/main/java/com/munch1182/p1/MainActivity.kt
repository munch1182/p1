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
import androidx.compose.ui.Modifier
import com.munch1182.p1.base.BaseActivity
import com.munch1182.p1.ui.PageAnimatedStyle
import com.munch1182.p1.ui.theme.P1Theme
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.generated.NavGraphs
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent(content = ::AppNavigation)
    }
}

/**
 * 导航
 */
@Composable
fun AppNavigation() {
    P1Theme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            DestinationsNavHost(
                navGraph = NavGraphs.root, // NavGraphs是一个ksp生成的类
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(innerPadding),
                defaultTransitions = PageAnimatedStyle
            )
        }
    }
}
