package com.munch1182.feature.bluetooth.connect

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.munch1182.core.base.NotSingleton
import com.munch1182.lib.bluetooth.BluetoothEnv
import com.munch1182.lib.bluetooth.classic.SPP_DEFAULT_UUID
import com.munch1182.lib.bluetooth.le.BLEDevice
import com.munch1182.lib.bluetooth.le.BLEServiceInfo
import com.munch1182.lib.bluetooth.le.BluetoothConnectState
import com.munch1182.lib.bluetooth.le.GattConnectProtocol
import com.munch1182.lib.bluetooth.le.IBLEDeviceManager
import com.munch1182.lib.bluetooth.le.collectServiceInfo
import com.munch1182.lib.common.closeQuietly
import com.munch1182.lib.common.launchIO
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class BluetoothConnectViewModel @Inject constructor(
    @param:NotSingleton private val manager: IBLEDeviceManager<String>
) : ViewModel() {

    private val _state = MutableStateFlow(BluetoothState())
    val state = _state.asStateFlow()
    private val _errMsg = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 1)
    val errMsg = _errMsg.asSharedFlow()

    fun toggleConnect(address: String) {
        _errMsg.tryEmit("")
        when (_state.value.connectState) {
            BluetoothConnectState.Disconnected -> startConnect(address)
            BluetoothConnectState.Disconnecting -> waitUntilDisconnected(address)
            BluetoothConnectState.Connecting, BluetoothConnectState.Connected -> startDisconnect(address)
        }
    }

    fun toggleSppConnect(address: String, sppUuid: String = SPP_DEFAULT_UUID.toString()) {
    }

    private fun waitUntilDisconnected(address: String) {}

    private fun startDisconnect(address: String) {
        viewModelScope.launchIO {
            _state.value = _state.value.copy(connectState = BluetoothConnectState.Disconnecting)
            manager.disconnect(address)
            val dev = manager.get(address)
            if (dev == null) {
                _state.value = _state.value.copy(connectState = BluetoothConnectState.Disconnected)
            } else {
                _state.value = _state.value.copy(connectState = dev.state.value)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startConnect(address: String) {
        viewModelScope.launchIO {
            val check = BluetoothAdapter.checkBluetoothAddress(address)
            if (!check) return@launchIO _errMsg.emit("无效的mac地址")
            val dev = BluetoothEnv.adapter?.getRemoteDevice(address) ?: return@launchIO _errMsg.emit("未找到设备")
            _state.value = _state.value.copy(connectState = BluetoothConnectState.Connecting)
            val connectDev = manager.connect(dev, viewModelScope).getOrNull()
            if (connectDev == null) {
                _errMsg.emit("连接失败")
                _state.value = _state.value.copy(connectState = BluetoothConnectState.Disconnected)
                return@launchIO
            }
            collectServices(connectDev)
            _state.value = _state.value.copy(connectState = connectDev.state.value)
        }
    }

    private suspend fun collectServices(dev: BLEDevice<String>) {
        // 这些操作应该放在BLEProtocol中实现， 但此次是用于展示，因此再执行一次
        val connector = dev.connector
        val mtu = connector.requestMtu(MTU_DEF)?.mtu ?: MTU_DEF // 协商mtu获取实际的值
        val service = if ((connector.discoverServices())?.isSuccess ?: false) {
            (connector.allServices() ?: emptyList()).collectServiceInfo()
        } else {
            emptyList()
        }
        _state.value = _state.value.copy(mtu = mtu, services = service)
    }

    private val protocol = GattConnectProtocol<String>()

    init {
        manager.registerProtocol(protocol)
    }

    override fun onCleared() {
        manager.closeQuietly() // 只在vm生命周期中使用, 配合IBLEDeviceManager的provider
    }
}

const val MTU_DEF = 517

data class BluetoothState(
    val connectState: BluetoothConnectState = BluetoothConnectState.Disconnected,
    val mtu: Int = MTU_DEF,
    val services: List<BLEServiceInfo> = emptyList(),
)