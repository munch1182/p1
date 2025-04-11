package com.munch1182.lib.helper.blue

import android.content.Context
import com.munch1182.lib.AppHelper
import com.munch1182.lib.base.log
import com.munch1182.lib.helper.blue.scan.ClassicScanner
import com.munch1182.lib.helper.blue.scan.LeScanner

/**
 * https://developer.android.google.cn/develop/connectivity/bluetooth?hl=zh_cn
 */
object BluetoothHelper : IBluetoothEnv by BluetoothEnv {
    internal val log = log()
    internal val ctx: Context get() = AppHelper

    private val LE = LeScanner()
    private val CLASSIC = ClassicScanner()
}