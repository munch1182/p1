package com.munch1182.core.base

import com.munch1182.lib.android.logger
import com.munch1182.lib.common.AnalyticsTracker

/**
 * [AnalyticsTracker]的实现
 *
 * 建议无论有没有实际实现功能, 都先定义
 */
object AppAnalytics : AnalyticsTracker {
    private val log = logger()
    override fun trackScreen(screenName: String, properties: Map<String, Any>?) {
        log.i("trackScreen: $screenName, properties: ${properties?.str()}")
    }

    override fun trackEvent(eventName: String, properties: Map<String, Any>?) {
        log.i("trackEvent: $eventName, properties: ${properties?.str()}")
    }

    override fun trackUserProperty(key: String, value: Any) {
        log.i("trackUserProperty: $key=$value")
    }

    override fun identify(userId: String, traits: Map<String, Any>?) {
        log.i("identify: $userId, traits: ${traits?.str()}")
    }

    override fun setUserProperties(properties: Map<String, Any>) {
        log.i("setUserProperties: properties: ${properties.str()}")
    }

    private fun Map<String, Any>.str(): String {
        return this.map { "${it.key}=${it.value}" }.joinToString(", ")
    }
}