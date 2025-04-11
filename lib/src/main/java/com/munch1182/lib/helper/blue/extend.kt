package com.munch1182.lib.helper.blue

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.LifecycleOwner

fun BluetoothHelper.find(mac: String, timeout: Long = 60L * 1000L, find: (dev: BluetoothDevice?) -> Unit) {}
fun BluetoothHelper.find(owner: LifecycleOwner, mac: String, find: (dev: BluetoothDevice?) -> Unit) {}
