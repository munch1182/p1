package com.munch1182.lib.helper.blue.scan

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import androidx.annotation.RequiresPermission
import com.munch1182.lib.base.newLog
import com.munch1182.lib.helper.ARDefaultSyncManager
import com.munch1182.lib.helper.ARManager
import com.munch1182.lib.helper.blue.BluetoothHelper
import com.munch1182.lib.helper.blue.IBluetoothAdapter
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

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

@FunctionalInterface
fun interface BluetoothScanResultListener {
    /**
     * 扫描到该设备
     */
    fun onScanned(result: ScanResult)
}

@FunctionalInterface
fun interface BluetoothScannedListener {
    /**
     * 扫描到该设备
     */
    fun onScanned(result: BluetoothDevice)
}

@FunctionalInterface
fun interface BluetoothScanningListener {
    fun onScanning(isScanning: Boolean)
}

interface BluetoothScanListener : BluetoothScannedListener, BluetoothScanningListener, BluetoothScanResultListener {
    /**
     * 调用扫描设备，但还未实际执行开始扫描
     */
    fun onPreScanStart() {}

    /**
     * 调用扫描设备成功
     */
    fun onScanStart() {}

    /**
     * 调用停止扫描，但还未实际执行停止扫描
     */
    fun onPreScanStop() {}

    /**
     * 调用停止扫描成功
     */
    fun onScanStop() {}
}

abstract class BaseScanner : IBluetoothScan, IBluetoothAdapter by BluetoothHelper, ARManager<BluetoothScanListener> by ARDefaultSyncManager() {
    protected val log = BluetoothHelper.log.newLog(this::class.java.simpleName)

    private val lock = ReentrantLock()
    private val scannedManager = ARDefaultSyncManager<BluetoothScannedListener>()
    private val scanningManager = ARDefaultSyncManager<BluetoothScanningListener>()
    private val scanResultManger = ARDefaultSyncManager<BluetoothScanResultListener>()
    private var _isScanning = false
        get() = lock.withLock { field }
        set(value) = lock.withLock {
            log.logStr("isScanning: $field -> $value")
            field = value
            scanDispatchCallback.onScanning(value)
        }

    protected val scanDispatchCallback = object : BluetoothScanListener {
        override fun onScanned(result: BluetoothDevice) {
            /*log.logStr("onScanned: ${result.address}")*/
            scannedManager.forEach { it.onScanned(result) }
            forEach { it.onScanned(result) }
        }

        override fun onScanned(result: ScanResult) {
            scanResultManger.forEach { it.onScanned(result) }
            forEach { it.onScanned(result) }
        }

        override fun onScanning(isScanning: Boolean) {
            scanningManager.forEach { it.onScanning(isScanning) }
            forEach { it.onScanning(isScanning) }
        }

        override fun onPreScanStart() {
            super.onPreScanStart()
            log.logStr("onPreScanStart")
            _isScanning = true
            forEach { it.onPreScanStart() }
        }

        override fun onPreScanStop() {
            super.onPreScanStop()
            log.logStr("onPreScanStop")
            forEach { it.onPreScanStop() }
        }

        override fun onScanStart() {
            super.onScanStart()
            log.logStr("onScanStart")
            forEach { it.onScanStart() }
        }

        override fun onScanStop() {
            super.onScanStop()
            log.logStr("onScanStop")
            _isScanning = false
            forEach { it.onScanStop() }
            scannedManager.clear()
            scanResultManger.clear()
        }
    }

    fun setScanResultListener(l: BluetoothScanResultListener) = scanResultManger.add(l)
    fun setScannedListener(l: BluetoothScannedListener) = scannedManager.add(l)
    fun addScanningListener(l: BluetoothScanningListener) = scanningManager.add(l)
    fun removeScanningListener(l: BluetoothScanningListener) = scanningManager.remove(l)
}