package com.munch1182.p1.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.munch1182.core.android.getDeviceInfo
import com.munch1182.p1.ui.theme.paddingPage
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph

@Destination<RootGraph>
@Composable
fun AboutScreen() {
    var str by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        str = getDeviceInfo()
    }

    Column(Modifier.paddingPage()) {
        Text(text = str)
    }
}