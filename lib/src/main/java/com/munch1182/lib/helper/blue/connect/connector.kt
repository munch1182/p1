package com.munch1182.lib.helper.blue.connect

import android.Manifest
import android.bluetooth.BluetoothAdapter
import androidx.annotation.RequiresPermission

interface IBluetoothConnect {
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun startConnect(mac: String)

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun stopConnect(mac: String)
}

@FunctionalInterface
fun interface OnConnectedListener {
    fun onConnected(mac: String)
}

fun interface OnConnectStateChangeListener {
    fun onConnectStateChange(mac: String, state: BluetoothConnectState)
}

sealed class BluetoothConnectState {
    data object Connecting : BluetoothConnectState()
    data object Connected : BluetoothConnectState()
    data object Disconnecting : BluetoothConnectState()
    data object Disconnected : BluetoothConnectState()

    companion object {
        fun from(value: Int) = when (value) {
            BluetoothAdapter.STATE_CONNECTING -> Connecting
            BluetoothAdapter.STATE_CONNECTED -> Connected
            BluetoothAdapter.STATE_DISCONNECTING -> Disconnecting
            BluetoothAdapter.STATE_DISCONNECTED -> Disconnected
            else -> throw UnsupportedOperationException()
        }
    }

    fun toInt() = when (this) {
        is Connecting -> BluetoothAdapter.STATE_CONNECTING
        is Connected -> BluetoothAdapter.STATE_CONNECTED
        is Disconnecting -> BluetoothAdapter.STATE_DISCONNECTING
        is Disconnected -> BluetoothAdapter.STATE_DISCONNECTED
    }

    val isConnecting get() = this is Connecting
    val isConnected get() = this is Connected
    val isDisconnecting get() = this is Disconnecting
    val isDisconnected get() = this is Disconnected
}