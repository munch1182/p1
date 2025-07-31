package com.munch1182.lib.helper

import android.content.Context
import android.location.LocationManager
import com.munch1182.lib.base.ctx

val locationManager get() = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
val isLocationProvider get() = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)