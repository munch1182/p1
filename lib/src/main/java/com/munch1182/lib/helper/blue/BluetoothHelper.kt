package com.munch1182.lib.helper.blue

import android.annotation.SuppressLint
import android.content.Context
import com.munch1182.lib.AppHelper
import com.munch1182.lib.base.log
import com.munch1182.lib.helper.blue.connect.IBluetoothConnect
import com.munch1182.lib.helper.blue.connect.LeConnector
import com.munch1182.lib.helper.blue.scan.BluetoothScanResultListener
import com.munch1182.lib.helper.blue.scan.BluetoothScannedListener
import com.munch1182.lib.helper.blue.scan.BluetoothScanningListener
import com.munch1182.lib.helper.blue.scan.ClassicScanner
import com.munch1182.lib.helper.blue.scan.IBluetoothLEScanCallback
import com.munch1182.lib.helper.blue.scan.IBluetoothScan
import com.munch1182.lib.helper.blue.scan.LeScanner

/**
 * https://developer.android.google.cn/develop/connectivity/bluetooth?hl=zh_cn
 */
@SuppressLint("MissingPermission")
object BluetoothHelper : IBluetoothEnv by BluetoothEnv, IBluetoothScan, IBluetoothLEScanCallback, IBluetoothConnect {
    internal val log = log()
    internal val ctx: Context get() = AppHelper

    private val LE = BlueLE()
    val CLASSIC = ClassicScanner()

    private class BlueLE(
        private val scan: LeScanner = LeScanner(),
        private val connector: LeConnector = LeConnector()
    ) : IBluetoothScan by scan, IBluetoothLEScanCallback by scan, IBluetoothConnect by connector

    override fun startScan() = LE.startScan()
    override fun stopScan() = LE.stopScan()
    override fun startConnect(mac: String) = LE.startConnect(mac)
    override fun stopConnect(mac: String) = LE.stopConnect(mac)

    /**
     * 当[stopScan]后，回调会被自动清除
     */
    override fun setScannedListener(l: BluetoothScannedListener): BluetoothHelper {
        LE.setScannedListener(l)
        return this
    }

    override fun setScanResultListener(l: BluetoothScanResultListener): BluetoothHelper {
        LE.setScanResultListener(l)
        return this
    }

    override fun addScanningListener(l: BluetoothScanningListener): BluetoothHelper {
        LE.addScanningListener(l)
        return this
    }

    override fun removeScanningListener(l: BluetoothScanningListener): BluetoothHelper {
        LE.removeScanningListener(l)
        return this
    }

}