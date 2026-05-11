package com.munch1182.p1.base

import com.munch1182.core.android.Log
import com.munch1182.core.common.AnalyticsTracker

object AppAnalytics : AnalyticsTracker {
    private const val TAG = "AppAnalytics"
    override fun trackScreen(screenName: String, properties: Map<String, Any>?) {
        Log.i(TAG, "trackScreen: $screenName, properties: ${properties?.str()}")
    }

    override fun trackEvent(eventName: String, properties: Map<String, Any>?) {
        Log.i(TAG, "trackEvent: $eventName, properties: ${properties?.str()}")
    }

    override fun trackUserProperty(key: String, value: Any) {
        Log.i(TAG, "trackUserProperty: $key=$value")
    }

    override fun identify(userId: String, traits: Map<String, Any>?) {
        Log.i(TAG, "identify: $userId, traits: ${traits?.str()}")
    }

    override fun setUserProperties(properties: Map<String, Any>) {
        Log.i(TAG, "setUserProperties: properties: ${properties.str()}")
    }

    private fun Map<String, Any>.str(): String {
        return this.map { "${it.key}=${it.value}" }.joinToString(", ")
    }
}