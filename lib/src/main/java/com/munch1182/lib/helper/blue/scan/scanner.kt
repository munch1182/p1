package com.munch1182.lib.helper.blue.scan

import android.Manifest
import android.bluetooth.BluetoothDevice
import androidx.annotation.RequiresPermission

interface IBluetoothScan {
    /**
     * 需要Manifest.permission.BLUETOOTH_SCAN权限
     * 如果未设置usesPermissionFlags="neverForLocation"，还需要定位权限
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun startScan()

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun stopScan()
}

interface BluetoothScanListener {
    /**
     * 调用扫描设备，但还未实际返回执行开始扫描
     */
    fun preScanStart() {}

    /**
     * 调用扫描设备成功
     */
    fun onScanStart()

    /**
     * 调用停止扫描，但还未实际执行停止扫描
     */
    fun preScanStop() {}

    /**
     * 调用停止扫描成功
     */
    fun onScanStop()

    /**
     * 扫描到该设备
     */
    fun onScanned(result: BluetoothDevice)
}