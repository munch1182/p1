package com.munch1182.lib.helper

import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.provider.Settings
import androidx.core.location.LocationManagerCompat
import com.munch1182.lib.base.ctx

object LocationHelper {

    val lm by lazy { ctx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager }

    val isGpsOpen: Boolean
        get() = lm?.let { LocationManagerCompat.isLocationEnabled(it) } ?: false

    val gpsOpenIntent: Intent
        get() = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
}