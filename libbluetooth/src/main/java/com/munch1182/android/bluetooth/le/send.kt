package com.munch1182.android.bluetooth.le

import android.bluetooth.BluetoothStatusCodes
import com.munch1182.android.lib.base.launchIO
import com.munch1182.android.lib.base.log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeoutException

/**
 * 数据发送类
 *
 * @see parsePackType
 */
abstract class BleCommandSender<T>(
    private val scope: CoroutineScope, private val config: Config = Config()
) {
    private val log = log()

    private val commandChannel = Channel<CommandContext<T>>(Channel.BUFFERED)
    private val responseChannel = Channel<ResponseData>(Channel.BUFFERED)

    // 命令存储
    private val pendingCommands = ConcurrentHashMap<CommandKey<T>, PendingCommand>()

    init {
        startCommandProcessor()
        startResponseProcessor()
    }

    /**
     * 解析数据类型 - 需要子类实现
     */
    abstract fun parsePackType(data: ByteArray): T?

    /**
     * 验证响应数据 - 可选实现
     */
    open fun validateResponse(commandType: T, responseData: ByteArray): Boolean = true

    /**
     * 处理无法匹配的响应 - 可选实现
     */
    open fun onUnmatchedResponse(data: ByteArray) {
        log.logStr("Received unmatched response")
    }

    /**
     * 发送命令并等待响应
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun <R> sendCommand(command: BleCommand<T, R>, timeoutMs: Long = config.defaultTimeoutMs): CommandResult<R> {
        val context = CommandContext(command = command, timeoutMs = timeoutMs)

        return withTimeoutOrNull(timeoutMs + config.defaultTimeoutMs) {
            commandChannel.send(context)
            context.resultDeferred.await() as CommandResult<R>
        } ?: CommandResult.Timeout("Command timeout after ${timeoutMs}ms")
    }

    /**
     * 发送命令但不等待响应
     */
    fun sendFireAndForget(command: BleCommand<T, Unit>) {
        scope.launchIO {
            val context = CommandContext(command = command, timeoutMs = 0)
            commandChannel.send(context)
        }
    }

    /**
     * 接收BLE数据
     */
    fun onDataReceived(mac: String, data: ByteArray, timestamp: Long = System.currentTimeMillis()) {
        scope.launchIO { responseChannel.trySend(ResponseData(mac, data, timestamp)) }
    }

    /**
     * 命令处理协程 - 支持优先级
     */
    private fun startCommandProcessor() {
        scope.launchIO {
            for (context in commandChannel) {
                try {
                    processCommand(context)
                } catch (e: Exception) {
                    log.logStr("Command processing failed: ${e.message}")
                    context.complete(CommandResult.Error("Processing failed: ${e.message}"))
                }
            }
        }
    }

    /**
     * 响应处理协程
     */
    private fun startResponseProcessor() {
        scope.launchIO {
            for (response in responseChannel) {
                try {
                    processResponse(response)
                } catch (e: Exception) {
                    log.logStr("Response processing failed: $e")
                }
            }
        }
    }

    private suspend fun processCommand(context: CommandContext<T>) {
        val command = context.command
        val sendResult = command.send()

        when (sendResult) {
            is CommandSendResult.Success<T, *> -> context.complete(CommandResult.Success(sendResult.data))

            is CommandSendResult.WaitingForResponse<T, *> -> {
                if (context.timeoutMs > 0) {
                    val key = CommandKey(command.type, sendResult.expectedRespType)

                    val timeoutJob = scope.launchIO {
                        delay(context.timeoutMs)
                        // 等待响应循环去处理，如果时间内没有被处理，则是超时 //@see startResponseProcessor
                        // 是否可以添加job，当完成时通过取消job的方式取消长时间等待？
                        if (pendingCommands.remove(key) != null) {
                            context.complete(CommandResult.Timeout("Command timeout"))
                        }
                    }
                    val pendingCommand = PendingCommand(context, timeoutJob)

                    pendingCommands[key] = pendingCommand
                    context.onComplete = {
                        timeoutJob.cancel()
                        pendingCommands.remove(key)
                    }
                } else {
                    context.complete(CommandResult.Success(Unit))
                }
            }

            is CommandSendResult.Failed<T, *> -> context.complete(CommandResult.Error(sendResult.reason))
        }
    }


    private suspend fun processResponse(response: ResponseData) {
        val responseType = parsePackType(response.data) ?: run {
            log.logStr("Cannot parse response type from data")
            onUnmatchedResponse(response.data)
            return
        }

        // 查找匹配的命令
        val matchingKeys = pendingCommands.keys.filter { it.expectedRespType == responseType }
        if (matchingKeys.isEmpty()) {
            onUnmatchedResponse(response.data)
            return
        }

        for (key in matchingKeys) {
            val pendingCommand = pendingCommands.get(key) ?: continue

            try {
                val command = pendingCommand.context.command
                if (validateResponse(responseType, response.data)) {
                    val result = command.handleResponseWithPart(response.data)
                    when (result) {
                        BleRespResult.NeedMoreData -> {} // 等待更多数据，则不进行任何处理；包的组合需要子类自行处理

                        is BleRespResult.Error -> {
                            pendingCommands.remove(key)
                            pendingCommand.context.complete(CommandResult.Error("bleRespResultError: ${result.message}"))
                        }

                        is BleRespResult.Full<*> -> {
                            pendingCommands.remove(key)
                            pendingCommand.context.complete(CommandResult.Success(result.data))
                        }
                    }
                } else {
                    pendingCommand.context.complete(CommandResult.Error("Response validation failed"))
                }
            } catch (e: Exception) {
                log.logStr("Response handling failed: ${e.message}")
                pendingCommand.context.complete(CommandResult.Error("Response handling failed: ${e.message}"))
            }
        }
    }

    /**
     * 清理资源
     */
    fun cleanup() {
        // 取消所有待处理命令
        pendingCommands.values.forEach {
            it.context.complete(CommandResult.Error("Sender cleaned up"))
        }
        pendingCommands.clear()

        // 关闭通道
        commandChannel.close()
        responseChannel.close()
    }

    data class Config(val defaultTimeoutMs: Long = 10000L)

    data class CommandContext<T>(
        val command: BleCommand<T, *>, val timeoutMs: Long,
        val resultDeferred: CompletableDeferred<CommandResult<*>> = CompletableDeferred(),
        var onComplete: (() -> Unit)? = null
    ) {
        fun complete(result: CommandResult<*>) {
            onComplete?.invoke()
            resultDeferred.complete(result)
        }
    }

    data class PendingCommand(val context: CommandContext<*>, val timeoutJob: Job)

    data class ResponseData(val mac: String, val data: ByteArray, val timestamp: Long) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ResponseData

            if (timestamp != other.timestamp) return false
            if (mac != other.mac) return false
            if (!data.contentEquals(other.data)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = timestamp.hashCode()
            result = 31 * result + mac.hashCode()
            result = 31 * result + data.contentHashCode()
            return result
        }

    }

    /**
     * 关联发送数据包类型和启动返回的数据包类型
     */
    data class CommandKey<T>(val commandType: T, val expectedRespType: T)
}

/**
 * BLE命令接口，自行实现从而无需绑定蓝牙类
 */
interface BleCommand<T, R> {
    /**
     * 当前发送的数据类型，用于区分命令
     */
    val type: T

    /**
     * 实际发送蓝牙数据操作
     *
     * 返回的是R，而不是[BlePartRespResult<R>]
     */
    suspend fun send(): CommandSendResult<T, R>

    /**
     * 处理响应数据
     */
    suspend fun handleResponse(data: ByteArray): R

    /**
     * 如果数据是分段的，重写此方法
     *
     * @see BleRespResult.NeedMoreData
     * @see BleRespResult.Full
     */
    suspend fun handleResponseWithPart(data: ByteArray): BleRespResult<R> {
        return BleRespResult.Full(handleResponse(data))
    }
}

/**
 * 蓝牙响应数据结构
 */
sealed class BleRespResult<out R> {
    /**
     * 数据完整，返回结果
     */
    data class Full<R>(val data: R) : BleRespResult<R>()

    /**
     * 数据不完整，还需要更多数据来组成包
     */
    object NeedMoreData : BleRespResult<Nothing>()
    data class Error(val message: String) : BleRespResult<Nothing>()
}

/**
 * 蓝牙数据发送结果
 */
sealed class CommandSendResult<T, R> {
    /**
     * 发送成功，无需等待数据返回
     */
    data class Success<T, R>(val data: R? = null) : CommandSendResult<T, R>()

    /**
     * 发送成功，需要等待数据返回
     *
     * @param expectedRespType 预期返回的数据类型
     * @see BleCommandSender.parsePackType
     */
    data class WaitingForResponse<T, R>(val expectedRespType: T) : CommandSendResult<T, R>()

    /**
     * 发送失败
     */
    data class Failed<T, R>(val reason: String) : CommandSendResult<T, R>()

    companion object {
        fun <T, R> fromBluetoothStatus(code: Int, successData: R? = null): CommandSendResult<T, R> {
            return when (code) {
                BluetoothStatusCodes.SUCCESS -> Success<T, R>(successData)
                BluetoothStatusCodes.ERROR_MISSING_BLUETOOTH_CONNECT_PERMISSION -> Failed("Missing Bluetooth connect permission")

                BluetoothStatusCodes.ERROR_DEVICE_NOT_BONDED -> Failed("Device not bonded")

                BluetoothStatusCodes.ERROR_PROFILE_SERVICE_NOT_BOUND -> Failed("Profile service not bound")

                BluetoothStatusCodes.ERROR_GATT_WRITE_NOT_ALLOWED -> Failed("GATT write not allowed")

                BluetoothStatusCodes.ERROR_GATT_WRITE_REQUEST_BUSY -> Failed("GATT write request busy")

                else -> Failed("Bluetooth error: $code")
            }
        }
    }
}

/**
 * 命令发送结果，包括响应时间
 */
sealed class CommandResult<out R> {
    data class Success<R>(val data: R) : CommandResult<R>()
    data class Error(val message: String) : CommandResult<Nothing>()
    data class Timeout(val reason: String) : CommandResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    val isTimeout: Boolean get() = this is Timeout

    fun getOrNull(): R? = (this as? Success)?.data
    fun getOrThrow(): R = when (this) {
        is Success -> data
        is Error -> throw IllegalStateException(message)
        is Timeout -> throw TimeoutException(reason)
    }

    override fun toString(): String {
        return when (this) {
            is Success -> "Success(data=${data.toString()})"
            is Error -> "Error(message=$message)"
            is Timeout -> "Timeout(reason=$reason)"
        }
    }
}