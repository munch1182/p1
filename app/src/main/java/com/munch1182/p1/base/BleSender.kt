package com.munch1182.p1.base

import android.bluetooth.BluetoothGattCharacteristic
import com.munch1182.android.lib.base.launchIO
import com.munch1182.android.lib.base.subArray
import com.munch1182.android.bluetooth.le.BLEConnector
import com.munch1182.android.bluetooth.le.BleCommand
import com.munch1182.android.bluetooth.le.BleCommandSender
import com.munch1182.android.bluetooth.le.CommandSendResult
import com.munch1182.android.lib.AppHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.coroutineContext

/**
 * 蓝牙数据解析同一处理
 */
object BleSender : BleCommandSender<Byte>(AppHelper) {

    private val bleRefs = ConcurrentHashMap<String, BleWriteRef>()

    fun send(address: String, data: ByteArray): Boolean {
        return bleRefs.get(address)?.let { it.connector.writeCharacteristic(it.write, data) } ?: false
    }

    override fun parsePackType(data: ByteArray): Byte? {
        return data.getOrNull(3)
    }

    override fun validateResponse(commandType: Byte, responseData: ByteArray): Boolean {
        return responseData[0] == 0xA8.toByte() && responseData[1] == 0x94.toByte()
    }

    suspend fun register(connector: BLEConnector, write: BluetoothGattCharacteristic) {
        val job = SupervisorJob()
        val mac = connector.dev.address
        bleRefs[mac] = BleWriteRef(job, connector, write)
        val ctx = CoroutineScope(coroutineContext + job)
        ctx.launchIO {
            connector.data.collect { onDataReceived(mac, it) }
        }
    }

    fun unregister(address: String) {
        bleRefs.remove(address)?.job?.cancel()
    }
}


sealed class IniResult {
    object Success : IniResult()
    object NoDiscoverServices : IniResult()
    object NoFindUUIDService : IniResult()
    object NoFindUUIDWrite : IniResult()
    object NoFindUUIDNotify : IniResult()
    object NoFindUUIDNotifyUUID : IniResult()
    object NoSetNotify : IniResult()

    val isSuccess get() = this is Success

    override fun toString(): String {
        return when (this) {
            is Success -> "Success"
            is NoDiscoverServices -> "NoDiscoverServices"
            is NoFindUUIDService -> "NoFindUUIDService"
            is NoFindUUIDWrite -> "NoFindUUIDWrite"
            is NoFindUUIDNotify -> "NoFindUUIDNotify"
            is NoFindUUIDNotifyUUID -> "NoFindUUIDNotifyUUID"
            is NoSetNotify -> "NoSetNotify"
        }
    }
}


class BleWriteRef(val job: Job, val connector: BLEConnector, val write: BluetoothGattCharacteristic)

private interface Command {
    fun newSend(vararg byte: Byte) = byteArrayOf(0xA8.toByte(), 0x94.toByte(), *byte)
}

class SnQuery(private val mac: String) : BleCommand<Byte, String>, Command {
    override val type: Byte get() = 0x06.toByte()
    private val waitRespDataType = 0x06.toByte()

    override suspend fun send(): CommandSendResult<Byte, String> {
        val data = newSend(1, 6)
        if (!BleSender.send(mac, data)) {
            return CommandSendResult.Failed("Failed to write characteristic")
        }
        return CommandSendResult.WaitingForResponse(waitRespDataType)
    }

    override suspend fun handleResponse(data: ByteArray): String {
        return String(data.subArray(4, data.size - 1))
    }
}