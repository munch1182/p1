package com.munch1182.lib.helper.blue

import android.Manifest
import android.bluetooth.BluetoothDevice
import androidx.annotation.RequiresPermission
import androidx.lifecycle.LifecycleOwner
import com.munch1182.lib.base.onShow
import com.munch1182.lib.helper.blue.scan.BluetoothScanResultListener
import com.munch1182.lib.helper.blue.scan.BluetoothScannedListener
import kotlinx.coroutines.withTimeout

/**
 * 当[BluetoothHelper.stopScan]自动移除[l]
 */
@RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
fun BluetoothHelper.scan(l: BluetoothScannedListener) {
    setScannedListener(l).startScan()
}

@RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
fun BluetoothHelper.scanResult(l: BluetoothScanResultListener) {
    setScannedListener(l).startScan()
}

@RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
suspend fun BluetoothHelper.find(mac: String, timeout: Long = 60L * 1000L, find: (dev: BluetoothDevice?) -> Unit) {
    scan { dev ->
        if (dev.address == mac) {
            stopScan()
            find(dev)
        }
    }
    withTimeout(timeout) { stopScan() }
}

@RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
suspend fun BluetoothHelper.find(owner: LifecycleOwner, mac: String, timeout: Long = 60L * 1000L, find: (dev: BluetoothDevice?) -> Unit) {
    find(mac, timeout, find)
    owner.lifecycle.onShow { if (!it) stopScan() }
}