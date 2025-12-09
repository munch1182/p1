package com.munch1182.lib.helper

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import androidx.core.content.ContextCompat
import com.munch1182.lib.AppHelper
import com.munch1182.lib.base.OnUpdateListener
import com.munch1182.lib.base.getParcelableCompat
import com.munch1182.lib.base.launchIO
import com.munch1182.lib.base.log
import com.munch1182.lib.base.toHexStr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import java.io.Closeable

/**
 * https://developer.android.google.cn/develop/connectivity/usb/host?hl=zh_cn
 */
object UsbHelper {

    const val ACTION_USB_PERMISSION = "com.munch1182.lib.helper.UsbHelper.USB_PERMISSION"
    private val log = log()

    private val _devState = ARDefaultManager<OnUpdateListener<UsbUpdate>>()
    val devState = _devState.asFlow()

    fun getDevs(vid: Int, pid: Int): List<UsbDevice>? {
        return usbManager.deviceList?.values?.filter { it.vendorId == vid && it.productId == pid }
    }

    fun registerUsbStateUpdate() {
        ContextCompat.registerReceiver(
            AppHelper, usbPermissionReceiver, IntentFilter().apply {
                addAction(ACTION_USB_PERMISSION)
                addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            }, ContextCompat.RECEIVER_EXPORTED
        )
    }

    fun unregisterUsbStateUpdate() {
        AppHelper.unregisterReceiver(usbPermissionReceiver)
    }

    val usbManager by lazy { AppHelper.getSystemService(UsbManager::class.java) }

    fun requestUsbPermission(dev: UsbDevice, reqCode: Int = 0) {
        if (hasPermission(dev)) return
        val intent = PendingIntent.getBroadcast(AppHelper, reqCode, Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE)

        usbManager.requestPermission(dev, intent)
    }

    fun hasPermission(dev: UsbDevice) = usbManager.hasPermission(dev)

    internal fun update(update: UsbUpdate) {
        log.logStr("state: $update")
        _devState.forEach { it.onUpdate(update) }
    }

    private val usbPermissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val dev = intent?.getParcelableCompat(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
            when (intent?.action) {
                ACTION_USB_PERMISSION -> {
                    if (dev != null) {
                        val isPermission = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                        update(UsbUpdate(dev, if (isPermission) State.PermissionGranted else State.PermissionDenied))
                    }
                }

                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    dev?.let { update(UsbUpdate(it, State.Detached)) }
                }
            }
        }
    }

    class UsbDeviceReceiver : BroadcastReceiver() {

        private val log = log()

        override fun onReceive(context: Context?, intent: Intent?) {
            val dev = intent?.getParcelableCompat(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
            val isPermission = intent?.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
            log.logStr("onReceive: ${intent?.action}: ${dev?.deviceName}: $isPermission")
            when (intent?.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    dev?.let { update(UsbUpdate(it, State.Attached)) }
                }
            }
        }
    }

    sealed class State {
        object Attached : State()
        object Detached : State()
        object PermissionGranted : State()
        object PermissionDenied : State()
    }

    data class UsbUpdate(val dev: UsbDevice, val state: State)

    fun connect(dev: UsbDevice, intf: UsbInterface, connection: UsbDeviceConnection, receiverBuff: ByteArray = ByteArray(4096)): UsbDataHelper? {
        val points = List(intf.endpointCount) { intf.getEndpoint(it) }
        val inEndPoint = points.firstOrNull { it.direction == UsbConstants.USB_DIR_IN }
        val outEndPoint = points.firstOrNull { it.direction == UsbConstants.USB_DIR_OUT }
        if (inEndPoint == null || outEndPoint == null) return null
        return UsbDataHelper(connection, inEndPoint, outEndPoint, receiverBuff)
    }
}

val UsbDevice.hasPermission get() = UsbHelper.hasPermission(this)

class UsbDataHelper(
    val connection: UsbDeviceConnection, val inEndPoint: UsbEndpoint, val outEndpoint: UsbEndpoint, private val receiverBuff: ByteArray = ByteArray(4096)
) : Closeable {
    sealed class ReceiveDataEvent {
        data class Data(val data: ByteArray) : ReceiveDataEvent() {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as Data
                return data.contentEquals(other.data)
            }

            override fun hashCode(): Int {
                return data.contentHashCode()
            }
        }

        object Error : ReceiveDataEvent()
        object ConnectionClosed : ReceiveDataEvent()

        override fun toString() = when (this) {
            ConnectionClosed -> "ConnectionClosed"
            is Data -> data.take(10).toByteArray().toHexStr()
            Error -> "Error"
        }
    }

    private var isRunning = true

    fun stopReceive() {
        isRunning = false
    }

    fun receiveAsFlow() = callbackFlow {
        val deniedTime = 30L
        launchIO {
            try {
                while (isRunning && connection.fileDescriptor >= 0) {
                    val read = try {
                        connection.bulkTransfer(inEndPoint, receiverBuff, receiverBuff.size, 1000)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        -1
                    }
                    when {
                        read > 0 -> {
                            send(ReceiveDataEvent.Data(receiverBuff.copyOfRange(0, read)))
                        }

                        read == 0 -> {
                            delay(deniedTime) // 没有数据，短暂休眠
                        }

                        read == -1 -> {
                            send(ReceiveDataEvent.Error)
                            delay(deniedTime)
                        }
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                if (isActive) send(ReceiveDataEvent.Error)
            } finally {
                connection.close()
                if (isActive) send(ReceiveDataEvent.ConnectionClosed)
            }
        }
        awaitClose {}
    }.flowOn(Dispatchers.IO)

    fun send(data: ByteArray, len: Int = data.size): Boolean {
        val send = connection.bulkTransfer(outEndpoint, data, len, 5000)
        return send == data.size
    }

    override fun close() {
        connection.close()
    }
}