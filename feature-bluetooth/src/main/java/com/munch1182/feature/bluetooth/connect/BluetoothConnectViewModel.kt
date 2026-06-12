package com.munch1182.feature.bluetooth.connect

import android.bluetooth.BluetoothAdapter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.munch1182.lib.bluetooth.BluetoothEnv
import com.munch1182.lib.bluetooth.le.BluetoothConnectState
import com.munch1182.lib.common.launchIO
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class BluetoothConnectViewModel : ViewModel() {

    private val _state = MutableStateFlow(BluetoothState())
    val state = _state.asStateFlow()
    private val _errMsg = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 1)
    val errMsg = _errMsg.asSharedFlow()

    fun toggleConnect(address: String) {
        val connectState = _state.value.connectState
        when (connectState) {
            BluetoothConnectState.Disconnected -> startConnect(address)
            BluetoothConnectState.Disconnecting -> waitUntilDisconnected(address)
            BluetoothConnectState.Connecting, BluetoothConnectState.Connected -> startDisconnect(address)
        }
    }

    private fun waitUntilDisconnected(address: String) {}

    private fun startDisconnect(address: String) {}

    private fun startConnect(address: String) {
        viewModelScope.launchIO {
            val check = BluetoothAdapter.checkBluetoothAddress(address)
            if (!check) return@launchIO _errMsg.emit("无效的mac地址")
            val dev = BluetoothEnv.adapter?.getRemoteDevice(address)
                ?: return@launchIO _errMsg.emit("未找到设备")
            // TODO: connect
        }
    }
}

data class BluetoothState(
    val connectState: BluetoothConnectState = BluetoothConnectState.Disconnected,
    val services: List<BLEServiceInfo> = emptyList(),
    val connectedService: BLEServiceInfo? = null,
)