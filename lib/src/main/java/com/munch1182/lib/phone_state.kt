package com.munch1182.lib

import android.content.Context
import android.provider.Settings

fun Context.isDeveloperMode(): Boolean {
    return Settings.Secure.getInt(
        contentResolver,
        Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
        0
    ) == 1
}