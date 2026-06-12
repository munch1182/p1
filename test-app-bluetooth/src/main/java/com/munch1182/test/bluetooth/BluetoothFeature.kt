package com.munch1182.test.bluetooth

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.munch1182.feature.bluetooth.connect.BluetoothConnect
import com.munch1182.feature.bluetooth.scan.BleScanViewModel
import com.munch1182.feature.bluetooth.scan.BluetoothScan

@SuppressLint("MissingPermission")
@Composable
fun BluetoothFeature() {
    var nav by remember { mutableStateOf<Nav>(Nav.Scan) }
    BackHandler(enabled = nav is Nav.Connect) {
        // 当用户左滑退出时，触发这里的代码
        nav = Nav.Scan
    }
    when (val state = nav) {
        is Nav.Scan -> BluetoothScan(
            onDeviceClick = { device ->
                nav = Nav.Connect(
                    address = device.address,
                    name = device.name ?: "未知设备"
                )
            }
        )

        is Nav.Connect -> BluetoothConnect(
            address = state.address,
            deviceName = state.name,
            onConnectToggle = { }
        )
    }
}

private sealed class Nav {
    data object Scan : Nav()
    data class Connect(val address: String, val name: String) : Nav()
}
