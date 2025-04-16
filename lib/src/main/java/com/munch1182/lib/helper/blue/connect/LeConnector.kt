package com.munch1182.lib.helper.blue.connect

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import com.munch1182.lib.base.Logger
import com.munch1182.lib.base.log
import com.munch1182.lib.base.newLog
import com.munch1182.lib.helper.blue.BluetoothHelper
import com.munch1182.lib.helper.blue.IBluetoothAdapter
import com.munch1182.lib.helper.blue.asBluetMac

@SuppressLint("MissingPermission")
class LeConnector : IBluetoothAdapter by BluetoothHelper, IBluetoothConnect {

    internal val log = log()
    private val manager = HashMap<String, LEConnectorImpl>()

    override fun startConnect(mac: String) {
        log.logStr("startConnect: $mac")
        val device = mac.asBluetMac()
        if (device == null) {
            log.logStr("$mac is not valid mac, abort connect")
            return
        }
        val impl = LEConnectorImpl(device, log.newLog(mac))
        impl.startConnect(mac)
        manager[mac] = impl
    }

    override fun stopConnect(mac: String) {
        log.logStr("stopConnect: $mac")
        if (manager.contains(mac)) {
            manager[mac]?.stopConnect(mac)
            manager.remove(mac)
        }
    }

    class LEConnectorImpl(private val device: BluetoothDevice, private val log: Logger) : IBluetoothConnect {

        private var connectState: BluetoothConnectState = BluetoothConnectState.Disconnected
        private var gatt: BluetoothGatt? = null
        private val gattCallback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                super.onConnectionStateChange(gatt, status, newState)
                log.logStr("onConnectionStateChange: $status, $newState")
                connectState = BluetoothConnectState.from(newState)
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                super.onServicesDiscovered(gatt, status)
                log.logStr("onServicesDiscovered: $status")
            }

            override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
                super.onCharacteristicWrite(gatt, characteristic, status)
                log.logStr("onCharacteristicWrite: $status")
            }

            override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
                super.onCharacteristicChanged(gatt, characteristic, value)
                log.logStr("onCharacteristicChanged: ${characteristic.uuid}")
            }
        }

        override fun startConnect(mac: String) {
            log.logStr("connectGatt")
            gatt = device.connectGatt(null, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        }

        override fun stopConnect(mac: String) {
            gatt?.disconnect()
        }

        private fun release() {
            gatt?.close()
            gatt = null
        }
    }
}