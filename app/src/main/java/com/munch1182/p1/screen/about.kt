package com.munch1182.p1.screen

import android.annotation.SuppressLint
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.munch1182.core.ui.ScrollPage
import com.munch1182.lib.android.NetworkMonitor
import com.munch1182.lib.android.getDeviceInfo
import com.munch1182.lib.android.versionCodeCompat
import com.munch1182.lib.android.versionName
import com.munch1182.p1.AppGraph
import com.ramcosta.composedestinations.annotation.Destination

@SuppressLint("MissingPermission")
@Destination<AppGraph>
@Composable
fun AboutScreen() {
    var str by remember { mutableStateOf("") }
    DisposableEffect(Unit) {
        NetworkMonitor.startMonitoring()
        onDispose { NetworkMonitor.stopMonitoring() }
    }

    LaunchedEffect(Unit) {
        val info = getDeviceInfo()
        val version = "Version: ${versionName}(${versionCodeCompat})"
        str = "$info\n\n$version"
    }

    val net by NetworkMonitor.networkStatus.collectAsState()

    ScrollPage {
        Text(text = str)
        Text(text = "Internet: ${net.type}, enable: ${net.isValidated}")
    }
}