package com.munch1182.lib.bluetooth.le

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic

/**
 * 作为一个蓝牙协议应该处理的蓝牙逻辑
 */
interface BLEProtocolProvider {
    /** 协议唯一标识 */
    val protocolID: Int

    /** 判断设备是否支持当前协议 */
    suspend fun isSupport(dev: BluetoothDevice, connector: BLEConnector): Boolean

    /** 协议连接初始化（如开启通知、协商等） */
    suspend fun connect(connector: BLEConnector): Result<Unit>

    /** 查找可写入的特征值 */
    suspend fun findWriter(connector: BLEConnector): WriterResult
}

/** BLE 数据发送器，通过 [BLEConnector.write] 发送数据 */
class BLESender(private val connector: BLEConnector, private val writer: BluetoothGattCharacteristic) : BLEDataSender {
    /** 写入数据到特征值 */
    override fun send(data: ByteArray): Result<Unit> {
        val result = connector.write(writer, data)
        return if (result == 0) {  // GATT_SUCCESS
            Result.success(Unit)
        } else {
            Result.failure(Exception("write failed: $result"))
        }
    }
}

/**
 * 最简协议：仅建立 GATT 连接，不进行任何业务初始化
 * 作为兜底协议，优先级最低
 */
object GattConnectProtocol : BLEProtocol<String> {
    override val protocolID = Int.MIN_VALUE
    override fun identifyType(data: ByteArray) = null
    override suspend fun isSupport(dev: BluetoothDevice, connector: BLEConnector) = true
    override suspend fun connect(connector: BLEConnector) = Result.success(Unit)
    override suspend fun findWriter(connector: BLEConnector) = WriterResult.WriterNotRequired
}

/** 协议接口，同时实现协议提供者和类型识别 */
// 如果需要数据解析，可以额外实现 BLETypeIdentifier
interface BLEProtocol<T> : BLEProtocolProvider, BLETypeIdentifier<T>

/** 写特征查找结果 */
sealed class WriterResult {
    /** 协议不需要写入 */
    object WriterNotRequired : WriterResult()

    /** 未找到可写入的特征 */
    object NotFound : WriterResult()

    /** 已找到可写入的特征 */
    data class HasWriter(val characteristic: BluetoothGattCharacteristic) : WriterResult()

    override fun toString() = when (this) {
        WriterNotRequired -> "WriterNotRequired"
        NotFound -> "NotFound"
        is HasWriter -> "HasWriter: ${characteristic.uuid}"
    }
}