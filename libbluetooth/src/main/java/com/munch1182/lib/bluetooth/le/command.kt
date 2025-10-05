package com.munch1182.lib.bluetooth.le

import android.bluetooth.BluetoothGattCharacteristic
import java.util.UUID


/**
 * 指令生成接口 - 用于创建自定义指令
 */
interface IBleCommandGenerator {
    /**
     * 生成指令数据
     */
    fun generateCommand(): ByteArray

    /**
     * 获取目标特征值UUID
     */
    fun getTargetCharacteristicUuid(): UUID

    /**
     * 获取写入类型
     */
    fun getWriteType(): Int = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT

    /**
     * 获取超时时间
     */
    fun getTimeoutMs(): Long = 5000

    /**
     * 获取重试次数
     */
    fun getRetryCount(): Int = 0

    /**
     * 获取指令ID（用于跟踪）
     */
    fun getCommandId(): String = UUID.randomUUID().toString()
}

/**
 * 数据解析接口 - 用于解析接收到的数据
 */
interface IBleDataParser {
    /**
     * 解析接收到的数据
     * @param data 原始字节数据
     * @return 解析后的对象，如果无法解析返回null
     */
    fun parseData(data: ByteArray): Any?

    /**
     * 检查是否支持解析此数据
     */
    fun canParse(data: ByteArray): Boolean

    /**
     * 获取支持的指令类型（用于路由解析）
     */
    fun getSupportedCommandType(): String
}

/**
 * 指令发送监听器
 */
interface IBleCommandListener {
    /**
     * 指令发送开始
     */
    fun onCommandStart(command: BleCommand)

    /**
     * 指令发送成功
     */
    fun onCommandSuccess(command: BleCommand, response: ByteArray? = null)

    /**
     * 指令发送失败
     */
    fun onCommandFailed(command: BleCommand, exception: Throwable)

    /**
     * 接收到数据
     */
    fun onDataReceived(data: ByteArray, parsedData: Any? = null)
}

/**
 * 基础BLE指令数据类
 */
class BleCommand(
    val data: ByteArray,
    val characteristicUuid: UUID,
    val writeType: Int = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT,
    val timeoutMs: Long = 5000,
    val retryCount: Int = 0,
    val id: String = UUID.randomUUID().toString(),
    val commandType: String = "default",
    val expectResponse: Boolean = false,
    val responseTimeoutMs: Long = 3000
)

/**
 * 指令发送结果
 */
class CommandSendResult(
    val command: BleCommand,
    val success: Boolean,
    val response: ByteArray? = null,
    val exception: Throwable? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 发送状态
 */
sealed class SendState {
    object Idle : SendState()
    object Sending : SendState()
    class Success(val data: ByteArray) : SendState()
    data class Error(val exception: Throwable) : SendState()
    object Timeout : SendState()
}