package com.munch1182.lib.helper.blue

import android.bluetooth.BluetoothDevice
import android.content.Context
import com.munch1182.lib.AppHelper
import com.munch1182.lib.base.log
import com.munch1182.lib.helper.blue.scan.BluetoothScanListener
import com.munch1182.lib.helper.blue.scan.ClassicScanner
import com.munch1182.lib.helper.blue.scan.LeScanner

/**
 * https://developer.android.google.cn/develop/connectivity/bluetooth?hl=zh_cn
 */
object BluetoothHelper : IBluetoothEnv by BluetoothEnv {
    internal val log = log()
    internal val ctx: Context get() = AppHelper

    private var _isScanning = false
    private val scanCallback = object : BluetoothScanListener {
        override fun preScanStart() {
            super.preScanStart()
            _isScanning = true
        }

        override fun preScanStop() {
            super.preScanStop()
        }

        override fun onScanStart() {
        }

        override fun onScanStop() {
            _isScanning = false
        }

        override fun onScanned(result: BluetoothDevice) {
        }
    }
    val isScanning get() = _isScanning
    
    val LE = LeScanner(scanCallback)
    val CLASSIC = ClassicScanner(scanCallback)
}