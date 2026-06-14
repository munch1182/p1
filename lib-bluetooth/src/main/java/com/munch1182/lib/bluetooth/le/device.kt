package com.munch1182.lib.bluetooth.le

import android.bluetooth.BluetoothDevice
import com.munch1182.lib.android.Log
import com.munch1182.lib.android.logger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.Closeable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * 代表一个已建立 GATT 连接的 BLE 设备。
 *
 * **生命周期**：从 [IBLEDeviceManager.connect] 成功返回开始，到连接断开（主动、异常）为止。
 * **断开后不可复用**：失效的实例无法重新连接，任何操作都会返回 [DataResult.Fail] 或 [Result.failure]。
 *
 * @param T 数据类型标识符类型（由协议定义）
 * @param dev 设备对象
 * @param connector 连接实现
 * @param identifier 协议类型定义
 * @param sender 数据发送实现
 * @param dataHelper 数据解析实现
 */
class BLEDevice<T : Any>(
    val dev: BluetoothDevice, //
    val connector: BLEConnector, //
    identifier: BLETypeIdentifier<T>, //
    sender: BLEDataSender, //
    private val dataHelper: BLEDataHelper<T> = BLEDataHelper(connector.connectScopeOrEmpty(), identifier, sender, connector),
) {

    /** 当前连接状态流 */
    val state = connector.connectState

    /**
     * 断开连接，断开之后不能再使用该对象
     */
    fun disconnect() = connector.disconnect()

    /** 发送协议命令并等待响应; 如果此对象已经无效, 则不会实际发送(但也不会报错) */
    suspend fun <R> send(cmd: BLECommand<T, R>, timeout: Duration = 15000.milliseconds) = dataHelper.send(cmd, timeout)

    /** 注册数据监听 */
    fun onType(target: T? = null, block: (ByteArray) -> Unit) = dataHelper.onType(target, block)

    /**
     * 等待设备断开连接
     *
     * 注意: 该回调判执行在[BLEConnector.connectScopeOrEmpty]协程中
     *
     * @param action 断开连接时的回调
     */
    fun onDisconnect(action: () -> Unit): Job {
        return connector.connectScopeOrEmpty().launch {
            connector.connectState.first { it == BluetoothConnectState.Disconnected }.also { action() }
        }
    }
}

/**
 * 管理设备连接、断开、数据发送、协议解析等处理
 */
interface IBLEDeviceManager<T : Any> : Closeable {

    /**
     * 执行实际连接和连接后数据相关对象的构建
     *
     * @param devScope 将其作为设备操作的scope, 当此scope被取消时, 连接和数据等操作也会被取消
     */
    suspend fun connect(dev: BluetoothDevice, devScope: CoroutineScope): Result<BLEDevice<T>>

    /**
     * 断开连接
     */
    fun disconnect(mac: String)

    /**
     * 获取设备对象
     */
    fun get(mac: String): BLEDevice<T>?

    /** 所有已连接设备列表 */
    val devs: List<BLEDevice<T>>

    /**
     * 发送协议实现
     *
     * @see BLEDataHelper.send
     */
    suspend fun <R> send(mac: String, cmd: BLECommand<T, R>, timeout: Duration = 3000.milliseconds): DataResult<R>

    /**
     * 获取设备连接状态
     *
     * @see BLEConnector.connectState
     */
    fun state(mac: String): Flow<BluetoothConnectState>?

    /**
     * 注册数据接收回调
     */
    fun onType(mac: String, target: T?, block: (ByteArray) -> Unit): () -> Unit

    /**
     * 注册协议
     */
    fun registerProtocol(protocol: BLEProtocol<T>)

    /**
     * 取消注册协议
     */
    fun unregisterProtocol(protocol: BLEProtocol<T>)
}

/**
 * 蓝牙设备管理的默认实现
 */
class DefaultBLEDeviceManager<T : Any> : IBLEDeviceManager<T> {

    companion object {
        private const val TAG = "DefaultBLEDeviceManager"
    }

    private val devsMap = ConcurrentHashMap<String, BLEDevice<T>>()
    private val protocols = CopyOnWriteArrayList<BLEProtocol<T>>()
    private val connectTask = ConcurrentHashMap<String, CompletableDeferred<Result<BLEDevice<T>>>>()

    override fun close() {
        devsMap.values.forEach { it.disconnect() }
        devsMap.clear()
        protocols.clear()
        connectTask.values.forEach { it.cancel() }
        connectTask.clear()
    }

    override fun registerProtocol(protocol: BLEProtocol<T>) {
        protocols.add(protocol)
    }

    override fun unregisterProtocol(protocol: BLEProtocol<T>) {
        protocols.remove(protocol)
    }

    private suspend fun findProtocol(dev: BluetoothDevice, connector: BLEConnector): BLEProtocol<T>? = protocols.firstOrNull { it.isSupport(dev, connector) }

    override suspend fun connect(dev: BluetoothDevice, devScope: CoroutineScope): Result<BLEDevice<T>> {
        val mac = dev.address
        val log = logger("connect-$mac")
        get(mac)?.let {
            log.e("$mac: 发起连接但设备已经连接, 返回该对象")
            return Result.success(it)
        }
        connectTask[mac]?.let {
            log.e("$mac: 发起连接但已经有连接任务在执行, 等待连接结果")
            return it.await()
        }
        val deferred = CompletableDeferred<Result<BLEDevice<T>>>()
        val existing = connectTask.putIfAbsent(mac, deferred) // 阻止并发重复创建
        if (existing != null) return existing.await()

        suspend fun doConnect(): Result<BLEDevice<T>> {
            val connector = BLEConnector(dev, devScope)

            fun cleanupAndFail(error: BLEConnectErr): Result<BLEDevice<T>> {
                connector.disconnect()
                log.e("$mac: 连接流程结束, 连接失败: $error")
                return Result.failure(BLEConnectException(error))
            }

            connector.connect()
            log.d("$mac: 开始连接流程, 发起连接, 等待连接结果")
            if (!connector.awaitConnected()) return cleanupAndFail(BLEConnectErr.Connect)
            if (connector.discoverServices()?.isSuccess != true) return cleanupAndFail(BLEConnectErr.DiscoverService)
            log.d("$mac: 连接成功, 发现服务成功")

            val protocol = findProtocol(dev, connector) ?: return cleanupAndFail(BLEConnectErr.FindProtocol)
            log.d("$mac: 找到协议: ${protocol.protocolId}, 执行协议处理")

            val protocolResult = protocol.connect(connector)
            if (protocolResult.isFailure) {
                connector.disconnect()
                return Result.failure(protocolResult.exceptionOrNull() ?: Exception("protocol connect failed"))
            }

            val sender = when (val writer = protocol.findWriter(connector)) {
                WriterResult.NotFound -> return cleanupAndFail(BLEConnectErr.FindWriter)
                is WriterResult.HasWriter -> BLESender(connector, writer.characteristic)
                WriterResult.WriterNotRequired -> BLENoOpSender()
            }

            val bleDevice = BLEDevice(
                dev = dev, //
                connector = connector, //
                identifier = protocol, //
                sender = sender
            )
            devsMap[mac] = bleDevice
            bleDevice.onDisconnect {
                log.d("$mac: 设备已断开连接, 移除持有对象")
                devsMap.remove(mac)
            }
            log.d("$mac: 连接流程结束, 连接成功")
            return Result.success(bleDevice)
        }

        return try {
            val result = try {
                doConnect()
            } catch (e: Exception) {
                Result.failure(e)  // 未预期的异常，转为失败结果
            }
            deferred.complete(result)
            result
        } finally {
            connectTask.remove(mac)
        }
    }

    override fun disconnect(mac: String) {
        val device = devsMap.remove(mac)
        Log.d(TAG, "$mac: 断开连接${device?.dev?.address?.let { " ($it)" } ?: ""}")
        device?.disconnect()
    }

    override fun get(mac: String): BLEDevice<T>? = devsMap[mac]

    override val devs: List<BLEDevice<T>> get() = devsMap.values.toList()

    override suspend fun <R> send(mac: String, cmd: BLECommand<T, R>, timeout: Duration): DataResult<R> {
        return get(mac)?.send(cmd, timeout) ?: DataResult.Fail("device not connect")
    }

    override fun state(mac: String): Flow<BluetoothConnectState>? = get(mac)?.state

    override fun onType(mac: String, target: T?, block: (ByteArray) -> Unit) = get(mac)?.onType(target, block) ?: {}
}

/**
 * 空操作的数据发送器，用于不需要写入特征的协议（如纯监听设备）
 */
class BLENoOpSender : BLEDataSender {
    override fun send(data: ByteArray): Result<Unit> = Result.success(Unit)
}

/**
 * 蓝牙连接错误类型
 */
sealed class BLEConnectErr {
    object Connect : BLEConnectErr()
    object DiscoverService : BLEConnectErr()
    object FindProtocol : BLEConnectErr()
    object FindWriter : BLEConnectErr()

    override fun toString() = when (this) {
        Connect -> "connect fail"
        DiscoverService -> "discover service fail"
        FindProtocol -> "find protocol fail"
        FindWriter -> "find writer fail"
    }
}

/**
 * 蓝牙连接异常
 */
data class BLEConnectException(val err: BLEConnectErr) : Exception(err.toString())