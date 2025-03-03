package com.munch1182.lib

import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.provider.Settings
import androidx.core.location.LocationManagerCompat

object LocationHelper {

    val lm by lazy { AppHelper.getSystemService(Context.LOCATION_SERVICE) as? LocationManager }

    val isGpsOpen: Boolean
        get() = lm?.let { LocationManagerCompat.isLocationEnabled(it) } ?: false

    fun openGPS(context: Context) {
        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
    }
}