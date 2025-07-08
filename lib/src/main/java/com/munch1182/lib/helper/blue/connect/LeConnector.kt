package com.munch1182.lib.helper.blue.connect

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import com.munch1182.lib.base.Logger
import com.munch1182.lib.base.log
import com.munch1182.lib.base.newLog
import com.munch1182.lib.helper.ARDefaultSyncManager
import com.munch1182.lib.helper.ARManager
import com.munch1182.lib.helper.blue.BluetoothHelper
import com.munch1182.lib.helper.blue.IBluetoothAdapter
import com.munch1182.lib.helper.blue.asBluetMac

@SuppressLint("MissingPermission")
class LeConnector : IBluetoothAdapter by BluetoothHelper, IBluetoothConnect, ARManager<OnConnectedListener> by ARDefaultSyncManager(), IBluetoothConnectCallback {

    internal val log = log()
    private val manager = HashMap<String, LEConnectorImpl>()
    private val onConnectedListener = OnConnectStateChangeListener { mac, state ->
        if (state.isConnected) {
            forEach { it.onConnected(mac) }
        }
    }

    override fun startConnect(mac: String) {
        log.logStr("startConnect: $mac")
        val device = mac.asBluetMac()
        if (device == null) {
            log.logStr("$mac is not valid mac, abort connect")
            return
        }
        if (manager[mac] != null) {
            if (manager[mac]?.isConnected == true) {
                return
            } else {
                manager[mac]?.stopConnect(mac)
            }
            return
        }
        val impl = LEConnectorImpl(device, onConnectedListener, log.newLog(mac))
        impl.startConnect(mac)
        manager[mac] = impl
    }

    fun gattOps(mac: String, ops: Array<GattOp<*>>) {
        manager[mac]?.gattOps(ops)
    }

    override fun stopConnect(mac: String) {
        log.logStr("stopConnect: $mac")
        if (manager.contains(mac)) {
            manager[mac]?.release()
            manager.remove(mac)
        }
    }

    override fun addConnectListener(l: OnConnectedListener): IBluetoothConnectCallback {
        add(l)
        return this
    }

    override fun removeConnectListener(l: OnConnectedListener): IBluetoothConnectCallback {
        remove(l)
        return this
    }

    sealed class GattOp<ANT>(val start: (BluetoothGatt?) -> Unit, val timeout: Long = 3000L, val wait: (ANT) -> Boolean) {
        class FindServices(wait: (List<BluetoothGattService>?) -> Boolean) : GattOp<List<BluetoothGattService>?>(start = { gatt -> gatt?.discoverServices() }, wait = wait)
        class SetCharacteristicNotification(value: BluetoothGattCharacteristic, enable: Boolean, wait: (BluetoothGattCharacteristic) -> Boolean) : GattOp<BluetoothGattCharacteristic>(start = { gatt -> gatt?.setCharacteristicNotification(value, enable) }, wait = wait)
    }

    class LEConnectorImpl(private val device: BluetoothDevice, private val callback: OnConnectStateChangeListener, private val log: Logger) : IBluetoothConnect {

        val isConnected get() = connectState.isConnected
        private var waitService: ((List<BluetoothGattService>?) -> Boolean)? = null
        private var waitCharacteristic: ((BluetoothGattCharacteristic) -> Boolean)? = null

        private var connectState: BluetoothConnectState = BluetoothConnectState.Disconnected
        private var gatt: BluetoothGatt? = null
        private val gattCallback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                super.onConnectionStateChange(gatt, status, newState)
                log.logStr("onConnectionStateChange: $status, $newState")
                connectState = BluetoothConnectState.from(newState)
                this@LEConnectorImpl.gatt = gatt
                callback.onConnectStateChange(device.address, connectState)
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                super.onServicesDiscovered(gatt, status)
                log.logStr("onServicesDiscovered: $status")
                this@LEConnectorImpl.gatt = gatt
                val service = gatt?.services
                waitService?.invoke(service)
            }

            override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
                super.onCharacteristicWrite(gatt, characteristic, status)
                log.logStr("onCharacteristicWrite: $status")
            }

            override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
                super.onCharacteristicChanged(gatt, characteristic, value)
                log.logStr("onCharacteristicChanged: ${characteristic.uuid}")
                waitCharacteristic?.invoke(characteristic)
            }
        }

        fun gattOps(ops: Array<GattOp<*>>) {
            val gatt = gatt ?: return
            ops.forEach {
                when (it) {
                    is GattOp.FindServices -> {
                        this.waitService = it.wait
                        it.start(gatt)
                    }

                    is GattOp.SetCharacteristicNotification -> {
                        it.start(gatt)
                    }
                }
            }
        }

        override fun startConnect(mac: String) {
            log.logStr("connectGatt")
            gatt = device.connectGatt(null, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        }

        override fun stopConnect(mac: String) {
            gatt?.disconnect()
        }

        fun release() {
            gatt?.disconnect()
            gatt?.close()
            gatt = null
        }


    }
}