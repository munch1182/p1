package com.munch1182.lib.helper.blue

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresPermission

interface IBluetoothAdapter {
    val bm: BluetoothManager
    val adapter: BluetoothAdapter?
}

interface IBluetoothEnv : IBluetoothAdapter {

    /*
     * 该设备是否支持蓝牙
     */
    val isBlueSupport get() = adapter != null

    /**
     * 该设备是否支持ble
     */
    val isBleSupport get() = BluetoothHelper.ctx.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)

    /**
     * 蓝牙是否打开
     */
    @get:RequiresPermission(Manifest.permission.BLUETOOTH)
    val isBlueOn get() = adapter?.isEnabled ?: false

    /**
     * 是否支持批处理扫描
     *
     * 如果支持，可以在扫描中设置[android.bluetooth.le.ScanSettings.Builder.setReportDelay]大于0
     * 则会回调[android.bluetooth.le.ScanCallback.onBatchScanResults]
     */
    val isScanBatchingSupported
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) adapter?.isLePeriodicAdvertisingSupported ?: false else false

    /**
     * 获取已配对的设备，android31之前不需要[Manifest.permission.BLUETOOTH_CONNECT]权限
     */
    @get:RequiresPermission(anyOf = [Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_CONNECT])
    val bondDevs get() = adapter?.bondedDevices ?: emptySet()

    /**
     * 调用该intent即可通过系统提示用户打开蓝牙
     */
    fun enableBlueIntent() = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
}

object BluetoothEnv : IBluetoothEnv {
    override val bm by lazy { BluetoothHelper.ctx.getSystemService(BluetoothManager::class.java) }
    override val adapter get() = bm.adapter
}