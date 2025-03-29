package com.munch1182.lib.base

import android.provider.Settings

fun isInDeveloperMode(): Boolean {
    return Settings.Secure.getInt(ctx.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1
}

