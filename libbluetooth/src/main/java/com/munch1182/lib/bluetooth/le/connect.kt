package com.munch1182.lib.bluetooth.le

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.os.Build
import android.os.Handler
import com.munch1182.lib.AppHelper
import com.munch1182.lib.base.launchIO
import com.munch1182.lib.base.log
import com.munch1182.lib.bluetooth.BluetoothIBluetoothEnv
import com.munch1182.lib.bluetooth.IBluetoothEnv
import com.munch1182.lib.bluetooth.le.BleConnectManager.disconnect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeoutException
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

object BleConnectManager {
    private val manager = ConcurrentHashMap<String, BLEConnector>()

    /**
     * 连接一个蓝牙设备，返回连接状态流
     *
     * 多次重复连接不会重复执行；如果需要新连接，需要先调用[disconnect]
     */
    fun connect(dev: BluetoothDevice, scope: CoroutineScope): Flow<BLEConnector.ConnectState> {
        val oldConnector = manager[dev.address]
        if (oldConnector != null) return oldConnector.state
        val connector = BLEConnector(dev, scope)
        manager[dev.address] = connector
        connector.connect()
        return connector.state
    }

    fun disconnect(mac: String) {
        manager.remove(mac)?.disconnect()
    }

    fun getConnector(mac: String): BLEConnector? = manager[mac]

    fun cleanup() {
        manager.values.forEach { it.disconnect() }
        manager.clear()
    }
}

@SuppressLint("MissingPermission")
class BLEConnector(val dev: BluetoothDevice, private var scope: CoroutineScope) : IBluetoothEnv by BluetoothIBluetoothEnv {

    private val log = log()
    private var innerScope = CoroutineScope(scope.coroutineContext + SupervisorJob() + Dispatchers.IO)
    private var _gatt: BluetoothGatt? = null
    private val _data = MutableSharedFlow<ByteArray>()
    private val _state = MutableStateFlow<ConnectState>(ConnectState.Disconnected)
    private val pendingGattOperations = ConcurrentHashMap<GattOpKey, Continuation<*>>()

    val data: Flow<ByteArray> = _data.asSharedFlow()
    val state: Flow<ConnectState> = _state.asStateFlow()
    val gatt: BluetoothGatt? get() = _gatt

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            innerScope.launch {
                val newState = when (newState) {
                    BluetoothProfile.STATE_CONNECTING -> ConnectState.Connecting
                    BluetoothProfile.STATE_DISCONNECTING -> ConnectState.Disconnecting
                    BluetoothProfile.STATE_CONNECTED -> ConnectState.Connected
                    BluetoothProfile.STATE_DISCONNECTED -> ConnectState.Disconnected
                    else -> {
                        log.logStr("onConnectionStateChange(${dev.address}):  status: ${status == BluetoothGatt.GATT_SUCCESS}, newState: $newState")
                        return@launch
                    }
                }
                log.logStr("onConnectionStateChange(${dev.address}):  status: ${status == BluetoothGatt.GATT_SUCCESS}, newState: $newState")
                when (newState) {
                    ConnectState.Connecting -> _state.emit(ConnectState.Connecting)
                    ConnectState.Disconnecting -> _state.emit(ConnectState.Disconnecting)
                    ConnectState.Connected -> _state.emit(ConnectState.Connected)
                    ConnectState.Disconnected -> {
                        _state.emit(ConnectState.Disconnected)
                        handleDisconnect()
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            innerScope.launch {
                log.logStr("onServicesDiscovered(${dev.address}):  status: ${status == BluetoothGatt.GATT_SUCCESS}")
                val result = ServicesDiscoveryResult(gatt.services ?: emptyList(), status)
                completePendingOp(
                    GattOpKey.DiscoverServices, if (status == BluetoothGatt.GATT_SUCCESS) result else BluetoothException("Service discovery failed with status: $status")
                )
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            // log.logStr("onCharacteristicChanged(${dev.address}):  value: ${value.toHexString()}")
            innerScope.launch { _data.emit(value) }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray, status: Int) {
            innerScope.launch {
                log.logStr("onCharacteristicRead(${dev.address}):  status: ${status == BluetoothGatt.GATT_SUCCESS}")
                val result = CharacteristicOperationResult(characteristic, status)
                completePendingOp(
                    GattOpKey.ReadCharacteristic(characteristic.uuid), if (status == BluetoothGatt.GATT_SUCCESS) result else BluetoothException("Characteristic read failed with status: $status")
                )
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            log.logStr("onCharacteristicWrite(${dev.address}):  status: ${status == BluetoothGatt.GATT_SUCCESS}")
            // 不能等待此回调，因为此回调可能比结果的回调更晚到达，导致结果被忽略
        }

        override fun onDescriptorRead(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int, value: ByteArray) {
            innerScope.launch {
                log.logStr("onDescriptorRead(${dev.address}):  status: ${status == BluetoothGatt.GATT_SUCCESS}")
                val result = DescriptorOperationResult(descriptor, status)
                completePendingOp(
                    GattOpKey.ReadDescriptor(descriptor.uuid), if (status == BluetoothGatt.GATT_SUCCESS) result else BluetoothException("Descriptor read failed with status: $status")
                )
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            innerScope.launch {
                log.logStr("onDescriptorWrite(${dev.address}):  status: ${status == BluetoothGatt.GATT_SUCCESS}")
                val result = DescriptorOperationResult(descriptor, status)
                completePendingOp(
                    GattOpKey.WriteDescriptor(descriptor.uuid), if (status == BluetoothGatt.GATT_SUCCESS) result else BluetoothException("Descriptor write failed with status: $status")
                )
            }
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
            innerScope.launch {
                log.logStr("onReadRemoteRssi(${dev.address}):  status: ${status == BluetoothGatt.GATT_SUCCESS}, rssi: $rssi")
                val result = RemoteRssiResult(rssi = rssi, status = status)
                completePendingOp(
                    GattOpKey.ReadRemoteRssi, if (status == BluetoothGatt.GATT_SUCCESS) result else BluetoothException("Read remote RSSI failed with status: $status")
                )
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            innerScope.launch {
                log.logStr("onMtuChanged(${dev.address}):  status: ${status == BluetoothGatt.GATT_SUCCESS}, mtu: $mtu")
                val result = MtuChangeResult(mtu = mtu, status = status)
                completePendingOp(
                    GattOpKey.RequestMtu, if (status == BluetoothGatt.GATT_SUCCESS) result else BluetoothException("MTU change failed with status: $status")
                )
            }
        }
    }

    // region Public API

    fun connect(handler: Handler? = null) {
        if (_gatt != null) return
        innerScope.cancel()
        innerScope = CoroutineScope(scope.coroutineContext + SupervisorJob() + Dispatchers.IO)
        innerScope.launch {
            try {
                log.logStr("connect(${dev.address})")
                _state.value = ConnectState.Connecting
                _gatt = dev.connectGatt(
                    AppHelper, false, gattCallback, BluetoothDevice.TRANSPORT_LE, BluetoothDevice.PHY_LE_1M_MASK, handler
                )
            } catch (_: Exception) {
                _state.value = ConnectState.Disconnected
            }
        }
    }

    fun disconnect() {
        innerScope.launchIO {
            try {
                log.logStr("disconnect(${dev.address})")
                if (_state.value == ConnectState.Disconnected || _state.value == ConnectState.Connecting) {
                    _state.emit(ConnectState.Disconnected)
                }
                _gatt?.disconnect()
            } finally {
                runCatching { _gatt?.close() }
                _gatt = null
            }
        }
    }

    /**
     * 发现设备服务并返回发现的结果
     */
    suspend fun discoverServices(timeoutMs: Long = 10000): ServicesDiscoveryResult = executeGattOperation(GattOpKey.DiscoverServices, timeoutMs) {
        _gatt?.discoverServices() ?: false
    }

    suspend fun readCharacteristic(characteristic: BluetoothGattCharacteristic, timeoutMs: Long = 5000): CharacteristicOperationResult = executeGattOperation(GattOpKey.ReadCharacteristic(characteristic.uuid), timeoutMs) {
        _gatt?.readCharacteristic(characteristic) ?: false
    }

    /**
     * 直接写入数据，不会监听[BluetoothGattCallback.onCharacteristicWrite]，因为该回调可能比[BluetoothGattCallback.onCharacteristicChanged]更晚回调导致该结果被忽略
     */
    @Suppress("DEPRECATION")
    fun writeCharacteristic(characteristic: BluetoothGattCharacteristic, data: ByteArray, timeoutMs: Long = 5000): Boolean {
        val gatt = _gatt ?: return false
        // log.logStr("write data(${dev.address}): ${data.toHexStrCompact()}")
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeCharacteristic(characteristic, data, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT) == BluetoothStatusCodes.SUCCESS
        } else {
            data.let { characteristic.value = it }
            gatt.writeCharacteristic(characteristic)
        }
    }

    suspend fun readDescriptor(descriptor: BluetoothGattDescriptor, timeoutMs: Long = 5000): DescriptorOperationResult = executeGattOperation(GattOpKey.ReadDescriptor(descriptor.uuid), timeoutMs) {
        _gatt?.readDescriptor(descriptor) ?: false
    }

    @Suppress("DEPRECATION")
    suspend fun writeDescriptor(descriptor: BluetoothGattDescriptor, value: ByteArray, timeoutMs: Long = 5000): DescriptorOperationResult = executeGattOperation(GattOpKey.WriteDescriptor(descriptor.uuid), timeoutMs) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                (_gatt?.writeDescriptor(descriptor, value) ?: 0) == BluetoothStatusCodes.SUCCESS
            } else {
                descriptor.value = value
                _gatt?.writeDescriptor(descriptor)
            }
        }.getOrNull() ?: false
    }

    suspend fun readRemoteRssi(timeoutMs: Long = 5000): RemoteRssiResult = executeGattOperation(GattOpKey.ReadRemoteRssi, timeoutMs) {
        _gatt?.readRemoteRssi() ?: false
    }

    suspend fun requestMtu(mtu: Int, timeoutMs: Long = 5000): MtuChangeResult = executeGattOperation(GattOpKey.RequestMtu, timeoutMs) { _gatt?.requestMtu(mtu) ?: false }

    suspend fun setCharacteristicNotification(characteristic: BluetoothGattCharacteristic, enable: Boolean, descriptorUuid: UUID, timeoutMs: Long = 3000): Boolean = withTimeoutOrNull(timeoutMs) {
        if (!(_gatt?.setCharacteristicNotification(characteristic, enable) ?: true)) {
            log.logStr("setCharacteristicNotification(${dev.address}):  enable: $enable, result: false")
            return@withTimeoutOrNull false
        }
        log.logStr("setCharacteristicNotification(${dev.address}):  enable: $enable, result: true")

        val descriptor = characteristic.getDescriptor(descriptorUuid) ?: return@withTimeoutOrNull true
        val value = when {
            enable -> BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            else -> BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
        }
        val result = writeDescriptor(descriptor, value, timeoutMs)
        log.logStr("writeDescriptor(${dev.address}):  enable: $enable, result: ${result.isSuccess}")
        result.status == BluetoothGatt.GATT_SUCCESS
    } ?: throw TimeoutException("Set characteristic notification timed out")

    // endregion

    // region Private methods

    private suspend fun <T> executeGattOperation(key: GattOpKey, timeoutMs: Long, operation: () -> Boolean): T = withTimeoutOrNull(timeoutMs) {
        suspendCoroutine { continuation ->
            pendingGattOperations[key] = continuation

            if (!operation()) {
                completePendingOp<T>(key, BluetoothException("GATT operation failed to start"))
            }
        }
    } ?: throw TimeoutException("GATT operation timed out after ${timeoutMs}ms")

    @Suppress("UNCHECKED_CAST")
    private fun <T> completePendingOperation(key: GattOpKey, result: Result<T>) {
        val continuation = pendingGattOperations.remove(key) as? Continuation<T>
        continuation?.let { cont ->
            innerScope.launch {
                result.fold(onSuccess = { cont.resume(it) }, onFailure = { cont.resumeWithException(it) })
            }
        }
    }

    private fun <T> completePendingOp(key: GattOpKey, value: T) {
        completePendingOperation(key, Result.success(value))
    }

    private fun <T> completePendingOp(key: GattOpKey, exception: Throwable) {
        completePendingOperation(key, Result.failure<T>(exception))
    }

    private fun handleDisconnect() {
        _gatt?.close()
        _gatt = null

        // Cancel all pending operations
        pendingGattOperations.values.forEach { continuation ->
            continuation.resumeWithException(BluetoothException("Disconnected during operation"))
        }
        pendingGattOperations.clear()

        innerScope.cancel()
    }

    // endregion

    // region Data classes and enums

    sealed class ConnectState {
        object Disconnected : ConnectState()
        object Connecting : ConnectState()
        object Connected : ConnectState()
        object Disconnecting : ConnectState()

        val isConnected get() = this is Connected
        val isDisconnecting get() = this is Disconnecting
        val isConnecting get() = this is Connecting
        val isDisconnected get() = this is Disconnected

        override fun toString(): String {
            return when (this) {
                Disconnected -> "Disconnected"
                Connecting -> "Connecting"
                Connected -> "Connected"
                Disconnecting -> "Disconnecting"
            }
        }
    }

    private sealed class GattOpKey {
        object DiscoverServices : GattOpKey()
        data class ReadCharacteristic(val uuid: UUID) : GattOpKey()

        // data class WriteCharacteristic(val uuid: UUID) : GattOpKey()
        data class ReadDescriptor(val uuid: UUID) : GattOpKey()
        data class WriteDescriptor(val uuid: UUID) : GattOpKey()
        object ReadRemoteRssi : GattOpKey()
        object RequestMtu : GattOpKey()
    }

    abstract class OpResult(val status: Int) {
        val isSuccess get() = status == BluetoothGatt.GATT_SUCCESS
    }

    class ServicesDiscoveryResult(val services: List<BluetoothGattService>, status: Int) : OpResult(status)

    class CharacteristicOperationResult(val characteristic: BluetoothGattCharacteristic, status: Int) : OpResult(status)

    class DescriptorOperationResult(val descriptor: BluetoothGattDescriptor, status: Int) : OpResult(status)

    class RemoteRssiResult(val rssi: Int, status: Int) : OpResult(status)

    class MtuChangeResult(val mtu: Int, status: Int) : OpResult(status)

    class BluetoothException(message: String) : Exception(message)

    // endregion
}