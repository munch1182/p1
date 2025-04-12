package com.munch1182.lib.helper.blue

import android.annotation.SuppressLint
import android.content.Context
import com.munch1182.lib.AppHelper
import com.munch1182.lib.base.log
import com.munch1182.lib.helper.blue.scan.BluetoothScannedListener
import com.munch1182.lib.helper.blue.scan.BluetoothScanningListener
import com.munch1182.lib.helper.blue.scan.ClassicScanner
import com.munch1182.lib.helper.blue.scan.IBluetoothScan
import com.munch1182.lib.helper.blue.scan.LeScanner

/**
 * https://developer.android.google.cn/develop/connectivity/bluetooth?hl=zh_cn
 */
@SuppressLint("MissingPermission")
object BluetoothHelper : IBluetoothEnv by BluetoothEnv, IBluetoothScan {
    internal val log = log()
    internal val ctx: Context get() = AppHelper

    private val LE = LeScanner()
    private val CLASSIC = ClassicScanner()

    override fun startScan() = LE.startScan()
    override fun stopScan() = LE.stopScan()

    /**
     * 当[stopScan]后，回调会被自动清除
     */
    fun setScannedListener(l: BluetoothScannedListener): BluetoothHelper {
        LE.setScannedListener(l)
        return this
    }

    fun addScanningListener(l: BluetoothScanningListener): BluetoothHelper {
        LE.addScanningListener(l)
        return this
    }

    fun removeScanningListener(l: BluetoothScanningListener): BluetoothHelper {
        LE.removeScanningListener(l)
        return this
    }

}