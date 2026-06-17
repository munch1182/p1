package com.munch1182.feature.bluetooth.connect

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.munch1182.core.base.NotSingleton
import com.munch1182.lib.bluetooth.BluetoothEnv
import com.munch1182.lib.bluetooth.classic.ClassicConnector
import com.munch1182.lib.bluetooth.classic.SPP_DEFAULT_UUID
import com.munch1182.lib.bluetooth.classic.connect
import com.munch1182.lib.bluetooth.le.BluetoothConnectState
import com.munch1182.lib.bluetooth.le.IBLEDeviceManager
import com.munch1182.lib.common.launchIO
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

@HiltViewModel
class BluetoothSppViewModel @Inject constructor(@param:NotSingleton private val manager: IBLEDeviceManager<String>) : ViewModel() {
    private val _state = MutableStateFlow(BluetoothSppState())
    private val _receiver = MutableSharedFlow<ByteArray>(
        replay = 0, extraBufferCapacity = 1
    )
    val state = _state.asStateFlow()
    val receiver = _receiver.asSharedFlow()
    private var connectJob: Job? = null
    private var connector: ClassicConnector? = null

    @SuppressLint("MissingPermission")
    fun connect(dev: BluetoothDevice, spp: UUID = SPP_DEFAULT_UUID) {
        close()
        connectJob = viewModelScope.launchIO {
            val device = manager.get(dev.address)
            if (device != null && dev.bondState != BluetoothDevice.BOND_BONDED) {
                BluetoothEnv.createBond(dev)
                if (dev.bondState != BluetoothDevice.BOND_BONDED) {
                    _state.value = _state.value.copy(msg = "绑定失败", connectState = BluetoothConnectState.Disconnected)
                    return@launchIO
                }
            }
            _state.value = _state.value.copy(msg = "", connectState = BluetoothConnectState.Connecting)
            val connector = dev.connect(viewModelScope, spp)
            val isConnected = connector != null
            _state.value = _state.value.copy(
                msg = if (isConnected) "连接成功" else "连接失败", connectState = if (isConnected) BluetoothConnectState.Connected else BluetoothConnectState.Disconnected
            )
            this@BluetoothSppViewModel.connector = connector
            connector?.receive?.collect(_receiver::tryEmit)
        }
    }

    fun send(data: ByteArray) {
        _state.value = _state.value.copy(msg = "Sending")
        viewModelScope.launchIO {
            delay(500L)
            connector?.send(data)
            _state.value = _state.value.copy(msg = "Send: ${data.toHexString()}")
        }
    }


    fun disconnect() {
        close()
        _state.value = _state.value.copy(msg = "Disconnect", connectState = BluetoothConnectState.Disconnected)
    }

    private fun close() {
        connectJob?.cancel()
        connectJob = null
        connector?.close()
        connector = null
    }
}

data class BluetoothSppState(
    val connectState: BluetoothConnectState = BluetoothConnectState.Disconnected, val msg: String = ""
)

data class BluetoothSppReceiver(val data: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BluetoothSppReceiver

        return data.contentEquals(other.data)
    }

    override fun hashCode(): Int {
        return data.contentHashCode()
    }
}