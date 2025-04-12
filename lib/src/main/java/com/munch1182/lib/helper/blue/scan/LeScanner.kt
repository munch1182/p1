package com.munch1182.lib.helper.blue.scan

import android.annotation.SuppressLint
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings

@SuppressLint("MissingPermission")
class LeScanner : BaseScanner() {

    private val callback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            log.logStr("Scan Result:  ${result?.device?.address}")
            val dev = result?.device ?: return
            scanDispatchCallback.onScanned(dev)
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            when (errorCode) {
                SCAN_FAILED_ALREADY_STARTED -> log.logStr("Scan Failed: SCAN_FAILED_ALREADY_STARTED.")
                SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> log.logStr("Scan Failed: SCAN_FAILED_APPLICATION_REGISTRATION_FAILED.")
                SCAN_FAILED_INTERNAL_ERROR -> log.logStr("Scan Failed: SCAN_FAILED_INTERNAL_ERROR.")
                SCAN_FAILED_FEATURE_UNSUPPORTED -> log.logStr("Scan Failed: SCAN_FAILED_FEATURE_UNSUPPORTED.")
                SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES -> log.logStr("Scan Failed: SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES.")
                SCAN_FAILED_SCANNING_TOO_FREQUENTLY -> log.logStr("Scan Failed: SCAN_FAILED_SCANNING_TOO_FREQUENTLY.")
                else -> log.logStr("Scan Failed: $errorCode.")
            }
            stopScan()
        }
    }

    private val defScanSet = ScanSettings.Builder()
        //高功耗模式，如果扫描时app不在前台，则此设置无效，会默认使用ScanSettings.SCAN_MODE_LOW_POWER 低功耗模式
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()

    override fun startScan() {
        log.logStr("Start Scan")
        val scanner = adapter?.bluetoothLeScanner
        if (scanner == null) {
            log.logStr("Start Scan but LeScanner is null, ignore.")
            return
        }
        scanDispatchCallback.onPreScanStart()
        scanner.startScan(null, defScanSet, callback)
        scanDispatchCallback.onScanStart()
    }

    override fun stopScan() {
        log.logStr("Stop Scan")
        scanDispatchCallback.onPreScanStop()
        adapter?.bluetoothLeScanner?.stopScan(callback)
        scanDispatchCallback.onScanStop()
    }
}