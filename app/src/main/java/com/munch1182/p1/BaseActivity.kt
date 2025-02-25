package com.munch1182.p1

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.ui.Modifier
import com.munch1182.p1.ui.theme.P1Theme


fun ComponentActivity.setContentWithBase(
    parent: CompositionContext? = null,
    content: @Composable () -> Unit
) {
    setContent(parent, {
        P1Theme {
            Scaffold(modifier = Modifier.fillMaxWidth()) { innerPadding ->
                Box(Modifier.padding(innerPadding)) {
                    content()
                }
            }
        }
    })
}