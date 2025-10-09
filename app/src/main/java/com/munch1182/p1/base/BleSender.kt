package com.munch1182.p1.base

import android.bluetooth.BluetoothGattCharacteristic
import com.munch1182.lib.AppHelper
import com.munch1182.lib.base.launchIO
import com.munch1182.lib.base.subArray
import com.munch1182.lib.bluetooth.le.BLEConnector
import com.munch1182.lib.bluetooth.le.BleCommand
import com.munch1182.lib.bluetooth.le.BleCommandSender
import com.munch1182.lib.bluetooth.le.CommandExecutionResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.coroutineContext

object BleSender : BleCommandSender<Byte>(AppHelper) {

    val bleRefs = ConcurrentHashMap<String, BleWriteRef>()
    override fun parseResponseType(data: ByteArray): Byte? {
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

    override suspend fun execute(): CommandExecutionResult<Byte, String> {
        val data = newSend(1, 6)
        val write = BleSender.bleRefs[mac] ?: return CommandExecutionResult.Failed("No write reference found")
        if (!write.connector.writeCharacteristic(write.write, data)) {
            return CommandExecutionResult.Failed("Failed to write characteristic")
        }
        return CommandExecutionResult.WaitingForResponse(waitRespDataType)
    }

    override suspend fun handleResponse(data: ByteArray): String {
        return String(data.subArray(4, data.size - 1))
    }
}