package com.munch1182.lib.helper.blue.scan

import android.Manifest
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import androidx.annotation.RequiresPermission
import com.munch1182.lib.base.newLog
import com.munch1182.lib.helper.blue.BluetoothHelper
import com.munch1182.lib.helper.blue.IBluetoothAdapter

class LeScanner(private val l: BluetoothScanListener? = null) : IBluetoothScan, IBluetoothAdapter by BluetoothHelper {

    private val log = BluetoothHelper.log.newLog("LeScanner")
    private val callback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            val dev = result?.device ?: return
            log.logStr("Scan Result: ${dev.address}")
            l?.onScanned(dev)
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            log.logStr("Scan Failed: $errorCode")
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun startScan() {
        log.logStr("Start Scan")
        l?.preScanStart()
        adapter?.bluetoothLeScanner?.startScan(callback)
        l?.onScanStart()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun stopScan() {
        log.logStr("Stop Scan")
        l?.preScanStop()
        adapter?.bluetoothLeScanner?.stopScan(callback)
        l?.onScanStop()
    }
}