package com.munch1182.lib.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import com.munch1182.lib.base.bm as bm2

interface IBluetoothEnv {
    val bm: BluetoothManager?
    val adapter: BluetoothAdapter? get() = bm?.adapter
}

object BluetoothIBluetoothEnv : IBluetoothEnv {
    private val _bm by lazy { bm2 }
    override val bm: BluetoothManager? get() = _bm
}