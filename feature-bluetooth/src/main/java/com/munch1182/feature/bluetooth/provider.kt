package com.munch1182.feature.bluetooth

import androidx.compose.runtime.Composable
import com.munch1182.feature.bluetooth.connect.BluetoothConnect
import com.munch1182.feature.bluetooth.scan.BluetoothScan
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.ExternalModuleGraph
import com.ramcosta.composedestinations.annotation.NavGraph
import com.ramcosta.composedestinations.annotation.parameters.CodeGenVisibility

@NavGraph<ExternalModuleGraph>
annotation class FeatureBluetoothGraph

@Destination<FeatureBluetoothGraph>(start = true, visibility = CodeGenVisibility.INTERNAL)
@Composable
internal fun BluetoothScanScreen() {
    BluetoothScan()
}

@Destination<FeatureBluetoothGraph>(visibility = CodeGenVisibility.INTERNAL)
@Composable
internal fun BluetoothConnectScreen(mac: String) {
    BluetoothConnect(mac)
}

