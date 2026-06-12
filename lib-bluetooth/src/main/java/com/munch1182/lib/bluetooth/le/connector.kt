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
import com.munch1182.lib.android.AppHelper
import com.munch1182.lib.android.LifecycleBoundScope
import com.munch1182.lib.android.logger
import com.munch1182.lib.common.launchIO
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * 蓝牙 GATT 操作封装，将回调异步操作转为协程同步调用。
 *
 * 每个设备应独立使用一个 BLEConnector 实例，所有操作线程安全。
 *
 * @param dev   目标蓝牙设备
 * @param scope 协程作用域，取消时会自动断开连接并释放资源
 */
@SuppressLint("MissingPermission")
class BLEConnector(
    val dev: BluetoothDevice,
    private val scope: CoroutineScope,
) : BLEDataReceiverProvider {

    companion object {
        private const val NAME_SERVER = "00001800-0000-1000-8000-00805F9B34FB"
        private const val NAME_CHAR = "00002A00-0000-1000-8000-00805F9B34FB"
        private const val RECONNECT_DELAY_MS = 500L // 等待底层释放资源
    }

    private val logger = logger()

    /** 当前连接状态 */
    private val _state = MutableStateFlow<BluetoothConnectState>(BluetoothConnectState.Disconnected)

    private val boundScope = LifecycleBoundScope(
        parentScope = scope, isActiveFlow = _state.map { it.isConnected },  // 转为是否连接的 Boolean 流
        scopeContextAddition = SupervisorJob() + CoroutineName("BLEConnector-${dev.address}")
    )

    /**
     * 返回一个连接状态范围内有效的[CoroutineScope], 其它状态范围返回一个不会执行的scope
     */
    fun connectScopeOrEmpty() = boundScope.currScopeOrEmpty()

    /**
     * 蓝牙连接状态
     */
    val connectState = _state.asStateFlow()

    /** 设备主动上报的数据流（通知/指示） */
    private val _dataReceiveFlow = MutableSharedFlow<ByteArray>(
        replay = 0, extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override fun receiveFlowProvider(): Flow<ByteArray> = _dataReceiveFlow

    // 保护 _gatt 的锁
    private val _gattLock = Any()
    private var _gatt: BluetoothGatt? = null

    private var connectionJob: Job? = null

    /** 内部 GATT 回调结果流，用于将异步回调转为挂起等待 */
    private val _gattResultsFlow = MutableSharedFlow<GattResult>(
        replay = 0, extraBufferCapacity = 16, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            val newStateEnum = when (newState) {
                BluetoothProfile.STATE_CONNECTING -> BluetoothConnectState.Connecting
                BluetoothProfile.STATE_DISCONNECTING -> BluetoothConnectState.Disconnecting
                BluetoothProfile.STATE_CONNECTED -> BluetoothConnectState.Connected
                BluetoothProfile.STATE_DISCONNECTED -> BluetoothConnectState.Disconnected
                else -> {
                    logger.d("onConnectionStateChange(${dev.address}): status=$newState, newState=$newState")
                    return
                }
            }
            val from = _state.value
            logger.d("onConnectionStateChange: status=$status, $from -> $newStateEnum")
            _state.value = newStateEnum
            scope.launch { _gattResultsFlow.emit(GattResult.ConnectResult(status, newStateEnum)) }
            if (newStateEnum.isDisconnected) disconnect()
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            scope.launch {
                logger.d("onServicesDiscovered: status=$status")
                _gattResultsFlow.emit(GattResult.DiscoverServices(status))
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            super.onDescriptorWrite(gatt, descriptor, status)
            scope.launch {
                logger.d("onDescriptorWrite: status=$status")
                _gattResultsFlow.emit(GattResult.WriteDescriptor(descriptor?.uuid, status))
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            logger.d("onCharacteristicChanged: ${value.toHexString()}")
            _dataReceiveFlow.tryEmit(value)
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            @Suppress("DEPRECATION") super.onCharacteristicChanged(gatt, characteristic)
            @Suppress("DEPRECATION") val value = characteristic?.value ?: return
            _dataReceiveFlow.tryEmit(value)
        }

        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            super.onMtuChanged(gatt, mtu, status)
            scope.launch {
                logger.d("onMtuChanged: status=$status, mtu=$mtu")
                _gattResultsFlow.emit(GattResult.MtuChanged(mtu, status))
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray, status: Int) {
            super.onCharacteristicRead(gatt, characteristic, value, status)
            scope.launch {
                logger.d("onCharacteristicRead: status=$status")
                if (characteristic.uuid.toString().equals(NAME_CHAR, true)) {
                    val name = String(value)
                    logger.d("onCharacteristicRead: name=$name")
                    _gattResultsFlow.emit(GattResult.ReadName(status, name))
                }
            }
        }
    }

    init {
        // 外部 scope 取消时自动断开连接
        scope.coroutineContext.job.invokeOnCompletion { disconnect() }
    }

    /**
     * 发起蓝牙连接，非阻塞。
     * 若当前已连接或正在连接，会先断开旧连接再重新连接。
     */
    fun connect(
        transport: Int = BluetoothDevice.TRANSPORT_LE, phy: Int = BluetoothDevice.PHY_LE_1M_MASK, handler: Handler? = null
    ) {
        connectionJob?.cancel()
        connectionJob = scope.launchIO {
            // 串行化：每次 connect() 取消旧 job，确保只有一个连接流程在执行
            if (_state.value.isConnecting || _state.value.isConnected) {
                logger.d("connect: already connecting/connected, disconnecting first")
                synchronized(_gattLock) {
                    _gatt?.disconnect()
                    _gatt?.close()
                    _gatt = null
                }
                _state.value = BluetoothConnectState.Disconnected
                delay(RECONNECT_DELAY_MS)
            }
            logger.d("connect")
            _state.value = BluetoothConnectState.Connecting
            val gatt = dev.connectGatt(AppHelper, false, callback, transport, phy, handler)
            synchronized(_gattLock) { _gatt = gatt }
            if (gatt == null) {
                logger.d("connectGatt failed")
                _state.value = BluetoothConnectState.Disconnected
            }
        }
    }

    /** 挂起等待连接成功，超时返回 false */
    suspend fun awaitConnected(timeout: Duration = 10000.milliseconds): Boolean {
        if (_state.value.isConnected) return true
        return waitForGattResult<GattResult.ConnectResult>(operation = { true }, name = "awaitConnected", timeout = timeout, filter = { true })?.isSuccess ?: false
    }

    /** 断开连接并释放 GATT 资源（同步执行，线程安全） */
    fun disconnect() {
        connectionJob?.cancel()
        if (_state.value.isDisconnected || _state.value.isDisconnecting) return
        synchronized(_gattLock) {
            _gatt?.disconnect()
            _gatt?.close()
            _gatt = null
        }
        _state.value = BluetoothConnectState.Disconnected
    }

    /** 执行服务发现，挂起直到完成或超时，返回结果或 null */
    suspend fun discoverServices(
        timeout: Duration = 15000.milliseconds, filter: (GattResult.DiscoverServices) -> Boolean = { true }
    ) = waitForGattResult(
        operation = { synchronized(_gattLock) { _gatt?.discoverServices() ?: false } }, name = "discoverServices", timeout = timeout, filter = filter
    )

    /** 根据正则表达式查找服务（线程安全） */
    fun findService(pattern: String): BluetoothGattService? {
        val regex = Regex(pattern, RegexOption.IGNORE_CASE)
        return synchronized(_gattLock) {
            _gatt?.services?.find { regex.containsMatchIn(it.uuid.toString()) }
        }
    }

    /** 在指定服务中根据正则查找特征 */
    fun findCharacteristic(service: BluetoothGattService, pattern: String): BluetoothGattCharacteristic? {
        val regex = Regex(pattern, RegexOption.IGNORE_CASE)
        return service.characteristics.find { regex.containsMatchIn(it.uuid.toString()) }
    }

    /** 根据服务 UUID 和特征正则查找特征 */
    /** 根据服务 UUID 正则和特征 UUID 正则查找特征 */
    fun findCharacteristic(service: String, pattern: String): BluetoothGattCharacteristic? {
        val server = findService(service) ?: return null
        return findCharacteristic(server, pattern)
    }

    /** 在特征中根据正则查找描述符 */
    fun findDescriptor(character: BluetoothGattCharacteristic, pattern: String): BluetoothGattDescriptor? {
        val regex = Regex(pattern, RegexOption.IGNORE_CASE)
        return character.descriptors.find { regex.containsMatchIn(it.uuid.toString()) }
    }

    /** 读取设备名称（通过服务），超时返回 null */
    suspend fun readName(
        timeout: Duration = 300.milliseconds, nameServer: String = NAME_SERVER, nameChar: String = NAME_CHAR
    ): String? {
        val service = findService(nameServer) ?: return null
        val characteristic = findCharacteristic(service, nameChar) ?: return null
        return waitForGattResult<GattResult.ReadName>(operation = { synchronized(_gattLock) { _gatt?.readCharacteristic(characteristic) ?: false } }, name = "readName", timeout = timeout, filter = { true })?.name
    }

    /**
     * 向设备请求 MTU 值，挂起直到结果或超时；
     *
     * 建议在连接成功后服务发现前就请求MTU，以加快特征值读取速度（如果过长);
     * */
    suspend fun requestMtu(
        mtu: Int = 512, timeout: Duration = 15000.milliseconds, filter: (GattResult.MtuChanged) -> Boolean = { true }
    ) = waitForGattResult(
        operation = { synchronized(_gattLock) { _gatt?.requestMtu(mtu) ?: false } }, name = "requestMtu", timeout = timeout, filter = filter
    )

    /** 开启或关闭特征的通知 */
    fun setNotification(characteristic: BluetoothGattCharacteristic, enable: Boolean) = synchronized(_gattLock) { _gatt?.setCharacteristicNotification(characteristic, enable) ?: false }

    /** 写入描述符，挂起直到写入完成或超时 */
    suspend fun writeDescriptor(
        descriptor: BluetoothGattDescriptor, value: ByteArray = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, timeout: Duration = 15000.milliseconds, filter: (GattResult.WriteDescriptor) -> Boolean = {
            descriptor.uuid.toString().equals(it.uuid?.toString(), true)
        }
    ) = waitForGattResult(
        operation = {
            synchronized(_gattLock) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    _gatt?.writeDescriptor(descriptor, value) == BluetoothStatusCodes.SUCCESS
                } else {
                    _gatt?.let {
                        @Suppress("DEPRECATION")
                        descriptor.value = value
                        @Suppress("DEPRECATION")
                        it.writeDescriptor(descriptor)
                    } == true
                }
            }
        }, name = "writeDescriptor", timeout = timeout, filter = filter
    )

    /** 向特征值写入数据，返回 0 成功，-1 失败 */
    fun write(
        writer: BluetoothGattCharacteristic, data: ByteArray, type: Int = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
    ): Int {
        return synchronized(_gattLock) {
            val gatt = _gatt ?: return -1
            val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeCharacteristic(writer, data, type)
            } else {
                @Suppress("DEPRECATION")
                writer.value = data
                writer.writeType = type
                @Suppress("DEPRECATION") if (gatt.writeCharacteristic(writer)) 0 else -1
            }
            logger.d("write: size=${data.size}, hex=${data.toHexString()}, success=${result == 0}")
            result
        }
    }

    /**
     * 内部通用方法：执行 GATT 操作并等待对应的回调结果。
     * 超时则返回 null。
     */
    private suspend inline fun <reified T : GattResult> waitForGattResult(
        crossinline operation: () -> Boolean, name: String, timeout: Duration, crossinline filter: (T) -> Boolean
    ): T? = withTimeoutOrNull(timeout) {
        val success = operation()
        logger.d("$name: operation success=$success")
        if (!success) return@withTimeoutOrNull null
        _gattResultsFlow.filterIsInstance<T>().filter(filter).first()
    }

    /** GATT 操作结果封装 */
    sealed class GattResult {
        abstract val status: Int
        open val isSuccess get() = status == BluetoothGatt.GATT_SUCCESS

        /** 连接状态变化结果 */
        data class ConnectResult(override val status: Int, val state: BluetoothConnectState) : GattResult() {
            override val isSuccess: Boolean get() = status == BluetoothGatt.GATT_SUCCESS && state.isConnected
        }

        /** 服务发现结果 */
        data class DiscoverServices(override val status: Int) : GattResult()

        /** 读取描述符结果 */
        data class ReadDescriptor(val uuid: UUID, val value: ByteArray, override val status: Int) : GattResult() {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as ReadDescriptor
                if (status != other.status) return false
                if (uuid != other.uuid) return false
                if (!value.contentEquals(other.value)) return false
                return true
            }

            override fun hashCode(): Int {
                var result = status
                result = 31 * result + uuid.hashCode()
                result = 31 * result + value.contentHashCode()
                return result
            }
        }

        /** 写入描述符结果 */
        data class WriteDescriptor(val uuid: UUID?, override val status: Int) : GattResult()

        /** MTU 变更结果 */
        data class MtuChanged(val mtu: Int, override val status: Int) : GattResult()

        /** 读取设备名称结果 */
        data class ReadName(override val status: Int, val name: String?) : GattResult()
    }
}

/** 蓝牙连接状态枚举 */
sealed class BluetoothConnectState {
    object Disconnected : BluetoothConnectState()
    object Connecting : BluetoothConnectState()
    object Connected : BluetoothConnectState()
    object Disconnecting : BluetoothConnectState()

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