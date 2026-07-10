package com.munch1182.feature.net

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.munch1182.core.ui.ButtonStateConfig
import com.munch1182.core.ui.StateButton
import com.munch1182.core.ui.theme.paddingPage
import com.munch1182.lib.android.NetworkMonitor
import com.munch1182.lib.android.currWifiIp4Address
import com.munch1182.lib.android.isUseWifi
import com.munch1182.lib.common.launchIO

@Composable
internal fun WebSocketScreen(net: NetworkMonitor = NetworkMonitor) {
    val server = remember { WebSocketServer(host = currWifiIp4Address()?.toString() ?: "0.0.0.0") }
    val scope = rememberCoroutineScope()
    val runState by server.state.collectAsStateWithLifecycle()
    var notice by remember { mutableStateOf("") }

    DisposableEffect(Unit) {
        net.startMonitoring()
        onDispose {
            net.stopMonitoring()
            scope.launchIO { server.close() }
        }
    }
    LaunchedEffect(runState) {
        notice = when (runState) {
            WebSocketServer.ServerState.RUNNING -> {
                "当前IP: http:/${currWifiIp4Address()}:${server.port}"
            }

            else -> ""
        }
    }
    Column(Modifier.paddingPage()) {
        val scheme = MaterialTheme.colorScheme

        StateButton(runState, config = {
            when (runState) {
                WebSocketServer.ServerState.STARTING ->
                    ButtonStateConfig("启动中", scheme.tertiary, scheme.onTertiary)

                WebSocketServer.ServerState.RUNNING ->
                    ButtonStateConfig("运行中", scheme.errorContainer, scheme.onSecondaryContainer)

                WebSocketServer.ServerState.STOPPING ->
                    ButtonStateConfig("关闭中", scheme.errorContainer, scheme.onErrorContainer)

                WebSocketServer.ServerState.STOPPED ->
                    ButtonStateConfig("点击开启", scheme.primary, scheme.onPrimary)
            }
        }) {
            com.munch1182.lib.android.Log.d("aaa", "state: $runState")
            when (runState) {
                WebSocketServer.ServerState.STARTING -> {}
                WebSocketServer.ServerState.RUNNING -> {
                    scope.launchIO { server.close() }
                    notice = ""
                }

                WebSocketServer.ServerState.STOPPING -> {}
                WebSocketServer.ServerState.STOPPED -> {
                    if (!net.networkStatus.value.isUseWifi) {
                        notice = "请先启用WIFI"
                        return@StateButton
                    }
                    scope.launchIO { server.run() }
                }
            }
        }
        Text(notice, color = if (server.isRunning) Color.Black else MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
    }
}