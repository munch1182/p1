package com.munch1182.lib.bluetooth.le

import android.bluetooth.BluetoothStatusCodes
import com.munch1182.lib.base.launchIO
import com.munch1182.lib.base.log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeoutException
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch

abstract class BleCommandSender<T>(
    private val scope: CoroutineScope, private val config: Config = Config()
) {
    private val log = log()

    @OptIn(ExperimentalAtomicApi::class)
    private val commandIdGenerator = AtomicLong(0L)

    private val commandChannel = Channel<CommandContext<T>>(Channel.BUFFERED)
    private val responseChannel = Channel<ResponseData>(Channel.BUFFERED)

    // 命令存储
    private val pendingCommands = ConcurrentHashMap<CommandKey<T>, PendingCommand>()

    init {
        startCommandProcessor()
        startResponseProcessor()
    }


    /**
     * 解析响应类型 - 需要子类实现
     */
    abstract fun parseResponseType(data: ByteArray): T?

    /**
     * 验证响应数据 - 可选实现
     */
    open fun validateResponse(commandType: T, responseData: ByteArray): Boolean = true

    /**
     * 处理无法匹配的响应 - 可选实现
     */
    open fun onUnmatchedResponse(responseType: T, data: ByteArray) {
        log.logStr("Received unmatched response of type: $responseType")
    }


    /**
     * 发送命令并等待响应
     */
    @OptIn(ExperimentalAtomicApi::class)
    @Suppress("UNCHECKED_CAST")
    suspend fun <R> sendCommand(command: BleCommand<T, R>, timeoutMs: Long = config.defaultTimeoutMs): CommandResult<R> {
        val commandId = commandIdGenerator.incrementAndFetch()
        val context = CommandContext(id = commandId, command = command, timeoutMs = timeoutMs)

        return withTimeoutOrNull(timeoutMs + config.defaultTimeoutMs) {
            commandChannel.send(context)
            context.resultChannel.receive() as CommandResult<R>
        } ?: CommandResult.Timeout("Command timeout after ${timeoutMs}ms")
    }

    /**
     * 发送命令但不等待响应
     */
    @OptIn(ExperimentalAtomicApi::class)
    fun sendFireAndForget(command: BleCommand<T, Unit>) {
        scope.launchIO {
            val context = CommandContext(id = commandIdGenerator.incrementAndFetch(), command = command, timeoutMs = 0)
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
        val executionResult = command.execute()

        when (executionResult) {
            is CommandExecutionResult.Success<T, *> -> {
                context.complete(CommandResult.Success(executionResult.data))
            }

            is CommandExecutionResult.WaitingForResponse<T, *> -> {
                if (context.timeoutMs > 0) {
                    val key = CommandKey(command.type, executionResult.expectedResponseType)
                    val pendingCommand = PendingCommand(context)

                    pendingCommands[key] = pendingCommand
                    scope.launchIO {
                        delay(context.timeoutMs)
                        if (pendingCommands.remove(key) != null) {
                            context.complete(CommandResult.Timeout("Command timeout"))
                        }
                    }
                } else {
                    context.complete(CommandResult.Success(Unit))
                }
            }

            is CommandExecutionResult.Failed<T, *> -> {
                context.complete(CommandResult.Error(executionResult.reason))
            }
        }
    }


    private suspend fun processResponse(response: ResponseData) {
        val responseType = parseResponseType(response.data) ?: run {
            log.logStr("Cannot parse response type from data")
            return
        }

        // 查找匹配的命令
        val matchingKeys = pendingCommands.keys.filter { it.expectedResponseType == responseType }

        if (matchingKeys.isEmpty()) {
            onUnmatchedResponse(responseType, response.data)
            return
        }

        for (key in matchingKeys) {
            val pendingCommand = pendingCommands.get(key) ?: continue

            try {
                val command = pendingCommand.context.command
                if (validateResponse(responseType, response.data)) {
                    val result = command.handleResponseWithPart(response.data)
                    when (result) {
                        BlePartRespResult.NeedMoreData -> {} // 等待更多数据
                        is BlePartRespResult.Error -> {
                            pendingCommands.remove(key)
                            pendingCommand.context.complete(CommandResult.Error("bleRespResultError: ${result.message}"))
                        }

                        is BlePartRespResult.Full<*> -> {
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
    suspend fun cleanup() {
        // 取消所有超时任务

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

    data class CommandContext<T>(val id: Long, val command: BleCommand<T, *>, val timeoutMs: Long, val createdAt: Long = System.currentTimeMillis(), val resultChannel: Channel<CommandResult<Any>> = Channel(1)) {
        suspend fun complete(result: CommandResult<*>) {
            @Suppress("UNCHECKED_CAST") resultChannel.send(result as CommandResult<Any>)
            resultChannel.close()
        }
    }

    data class PendingCommand(
        val context: CommandContext<*>, val registeredAt: Long = System.currentTimeMillis()
    )

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

    data class CommandKey<T>(
        val commandType: T, val expectedResponseType: T
    )
}

/**
 * BLE命令接口
 */
interface BleCommand<T, R> {
    val type: T

    /**
     * 返回的是R，而不是[BlePartRespResult<R>]
     */
    suspend fun execute(): CommandExecutionResult<T, R>
    suspend fun handleResponse(data: ByteArray): R

    suspend fun handleResponseWithPart(data: ByteArray): BlePartRespResult<R> {
        return BlePartRespResult.Full(handleResponse(data))
    }
}

sealed class BlePartRespResult<out R> {
    data class Full<R>(val data: R) : BlePartRespResult<R>()
    object NeedMoreData : BlePartRespResult<Nothing>()
    data class Error(val message: String) : BlePartRespResult<Nothing>()
}

sealed class CommandExecutionResult<T, R> {
    data class Success<T, R>(val data: R? = null) : CommandExecutionResult<T, R>()
    data class WaitingForResponse<T, R>(val expectedResponseType: T) : CommandExecutionResult<T, R>()
    data class Failed<T, R>(val reason: String) : CommandExecutionResult<T, R>()

    companion object {
        fun <T, R> fromBluetoothStatus(code: Int, successData: R? = null): CommandExecutionResult<T, R> {
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
 * 命令发送结果
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