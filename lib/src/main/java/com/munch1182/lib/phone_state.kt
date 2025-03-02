package com.munch1182.lib

import android.provider.Settings

fun isDeveloperMode(): Boolean {
    return Settings.Secure.getInt(
        ctx.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0
    ) == 1
}