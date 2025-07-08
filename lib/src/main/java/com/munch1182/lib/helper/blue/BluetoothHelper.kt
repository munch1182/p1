package com.munch1182.lib.helper.blue

import com.munch1182.lib.AppHelper
import com.munch1182.lib.base.Logger
import com.munch1182.lib.helper.blue.BluetoothHelperHelper.LeConnector
import com.munch1182.lib.helper.blue.BluetoothHelperHelper.LeScanner
import com.munch1182.lib.helper.blue.connect.IBluetoothConnect
import com.munch1182.lib.helper.blue.connect.IBluetoothConnectCallback
import com.munch1182.lib.helper.blue.connect.LeConnector
import com.munch1182.lib.helper.blue.connect.LeConnector.GattOp
import com.munch1182.lib.helper.blue.scan.ClassicScanner
import com.munch1182.lib.helper.blue.scan.IBluetoothLEScanCallback
import com.munch1182.lib.helper.blue.scan.IBluetoothScan
import com.munch1182.lib.helper.blue.scan.IBluetoothScanCallback
import com.munch1182.lib.helper.blue.scan.LeScanner

internal object BluetoothHelperHelper {

    val log = Logger("BluetoothHelper")
    val ctx = AppHelper

    internal val LeScanner = LeScanner()
    internal val LeConnector = LeConnector()
}

private object BluetoothLEHelper : IBluetoothScan by LeScanner, IBluetoothScanCallback by LeScanner, IBluetoothLEScanCallback by LeScanner, IBluetoothConnect by LeConnector, IBluetoothConnectCallback by LeConnector {
    internal val connector: LeConnector get() = LeConnector
}

abstract class BluetoothSingleHelper(
    protected val scan: IBluetoothScan, protected val connector: IBluetoothConnect, scanCallback: IBluetoothScanCallback, connectCallback: IBluetoothConnectCallback
) : IBluetoothEnv by BluetoothEnv, IBluetoothScan by scan, IBluetoothScanCallback by scanCallback, IBluetoothConnect by connector, IBluetoothReceiverListener by BluetoothEnv, IBluetoothConnectCallback by connectCallback

/**
 * 默认实现为BLE
 *
 * https://developer.android.google.cn/develop/connectivity/bluetooth?hl=zh_cn
 */
object BluetoothHelper : BluetoothSingleHelper(BluetoothLEHelper, BluetoothLEHelper, BluetoothLEHelper, BluetoothLEHelper), IBluetoothLEScanCallback by BluetoothLEHelper {
    val CLASSIC = ClassicScanner()

    fun gattOps(mac: String, ops: Array<GattOp<*>>) {
        (this.connector as? BluetoothLEHelper)?.connector?.gattOps(mac, ops)
    }
}