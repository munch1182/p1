package com.munch1182.android.lib.base

import android.Manifest
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.telephony.SubscriptionManager
import androidx.annotation.RequiresPermission

fun isDeveloperMode(): Boolean {
    return Settings.Secure.getInt(
        ctx.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0
    ) == 1
}

val sm get() = ctx.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager

@RequiresPermission(allOf = [Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_PHONE_NUMBERS])
fun getPhoneNumbers(): List<String>? {
    return sm.activeSubscriptionInfoList?.mapNotNull {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            sm.getPhoneNumber(it.subscriptionId)
        } else {
            @Suppress("DEPRECATION") it.number
        }.takeIf { i -> i.isNotEmpty() }
    }
}