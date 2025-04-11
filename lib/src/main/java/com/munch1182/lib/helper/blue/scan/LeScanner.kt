package com.munch1182.lib.helper.blue.scan

import android.Manifest
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import androidx.annotation.RequiresPermission

class LeScanner : BaseScanner() {

    private val callback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            val dev = result?.device ?: return
            log.logStr("Scan Result: ${dev.address}")
            scanDispatchCallback.onScanned(dev)
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            log.logStr("Scan Failed: $errorCode")
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun startScan() {
        log.logStr("Start Scan")
        scanDispatchCallback.onPreScanStart()
        adapter?.bluetoothLeScanner?.startScan(callback)
        scanDispatchCallback.onScanStart()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun stopScan() {
        log.logStr("Stop Scan")
        scanDispatchCallback.onPreScanStop()
        adapter?.bluetoothLeScanner?.stopScan(callback)
        scanDispatchCallback.onScanStop()
    }
}