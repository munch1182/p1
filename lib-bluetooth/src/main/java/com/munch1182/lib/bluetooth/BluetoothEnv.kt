package com.munch1182.lib.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Intent
import androidx.annotation.RequiresPermission
import com.munch1182.lib.android.AppHelper

/**
 * 提供蓝牙环境, 主要是[BluetoothManager]和[BluetoothAdapter]
 */
interface IBluetoothEnv {
    /**
     * 获取[BluetoothManager]
     */
    val bm: BluetoothManager?

    /**
     * 获取[BluetoothAdapter]
     */
    val adapter: BluetoothAdapter? get() = bm?.adapter

    /**
     * 获取gatt蓝牙设备, 需要蓝牙权限
     *
     * 如果要获取非gatt蓝牙设备 @see BluetoothProfileCache
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun getConnectedDevs() = bm?.getConnectedDevices(BluetoothProfile.GATT)

}

/**
 * 请求打开蓝牙开关的intent
 */
val enableBluetoothIntent get() = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)

/**
 * 实现一个[IBluetoothEnv]
 */
object BluetoothEnv : IBluetoothEnv {
    private val _bm by lazy { AppHelper.getSystemService(BluetoothManager::class.java) }
    override val bm: BluetoothManager? get() = _bm
}
