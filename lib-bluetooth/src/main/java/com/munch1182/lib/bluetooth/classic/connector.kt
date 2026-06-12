package com.munch1182.lib.bluetooth.classic

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import com.munch1182.lib.android.logger
import com.munch1182.lib.common.closeQuietly
import com.munch1182.lib.common.launchIO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import java.io.Closeable
import java.io.IOException
import java.util.UUID

val SPP_DEFAULT_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

/**
 * 经典蓝牙连接操作句柄，提供发送/接收/关闭。
 */
interface ClassicConnector : Closeable {
    fun send(data: ByteArray): Result<Unit>
    val receive: Flow<ByteArray>
}

/**
 * 连接经典蓝牙设备，连接成功返回 [ClassicConnector]。
 *
 * @param parentScope 父协程，用于控制生命周期
 * @param uuid 连接的目标 UUID，默认为 SPP
 * @param buffLen 每次接收缓冲区大小，默认 1024
 */
fun BluetoothDevice.connect(
    parentScope: CoroutineScope,
    uuid: UUID = SPP_DEFAULT_UUID,
    buffLen: Int = 1024
): ClassicConnector? {
    try {
        val socket = createRfcommSocketToServiceRecord(uuid)
        socket.connect()
        val connectScope = CoroutineScope(parentScope.coroutineContext + SupervisorJob())
        return ClassicConnectorImpl(connectScope, buffLen, socket)
    } catch (e: IOException) {
        return null
    }
}

private class ClassicConnectorImpl(
    private val connectScope: CoroutineScope,
    private val buffLen: Int,
    private val socket: BluetoothSocket
) : ClassicConnector {
    private val ous = socket.outputStream
    private val ins = socket.inputStream
    private val log = logger()

    private val receiveFlow = MutableSharedFlow<ByteArray>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    init {
        log.log("connect success, start receive stream(buffLen: $buffLen)")
        connectScope.launchIO {
            val buff = ByteArray(buffLen)
            try {
                while (isActive) {
                    val len = ins.read(buff) // 阻塞读取, 如果未主动关闭会一直等待
                    if (len <= 0) { // 等于-1: 已断开连接或者关闭流
                        break
                    }
                    receiveFlow.emit(buff.copyOfRange(0, len))
                }
            } catch (e: IOException) { // 安全退出
            } finally {
                log.log("receive stream closed")
                close()
            }
        }
    }

    override fun send(data: ByteArray): Result<Unit> {
        if (!connectScope.isActive) return Result.failure(IOException("connection closed"))
        return try {
            ous.write(data)
            ous.flush()
            Result.success(Unit)
        } catch (e: IOException) {
            log.log("send error: ${e.message}")
            Result.failure(e)
        }
    }

    override val receive: Flow<ByteArray> = receiveFlow.asSharedFlow()

    override fun close() {
        if (connectScope.isActive) connectScope.cancel()
        ins.closeQuietly()
        ous.closeQuietly()
        socket.closeQuietly()
        log.log("close connect")
    }
}