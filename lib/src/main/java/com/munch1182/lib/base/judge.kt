package com.munch1182.lib.base

import android.content.Context
import android.location.LocationManager

fun isGpsOpen(lm: LocationManager? = locManager) = runCatching { lm?.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrNull() ?: false

val locManager get() = ctx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager?

fun isBluetoothOpen() = runCatching { bm?.adapter?.isEnabled }.getOrNull() ?: false

val bm get() = ctx.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager?