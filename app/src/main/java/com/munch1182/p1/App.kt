package com.munch1182.p1

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.ui.Modifier
import com.munch1182.p1.ui.theme.P1Theme

class App : Application()


fun ComponentActivity.setContentWithBase(
    parent: CompositionContext? = null,
    content: @Composable (Modifier) -> Unit
) {
    setContent(parent) {
        P1Theme {
            Scaffold(modifier = Modifier.fillMaxWidth()) { innerPadding ->
                content(Modifier.padding(innerPadding))
            }
        }
    }
}