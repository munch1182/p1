package com.munch1182.core.common

interface AnalyticsTracker {
    fun trackScreen(screenName: String, properties: Map<String, Any>? = null)
    fun trackEvent(eventName: String, properties: Map<String, Any>? = null)
    fun trackUserProperty(key: String, value: Any)
    fun identify(userId: String, traits: Map<String, Any>? = null)
    fun setUserProperties(properties: Map<String, Any>)
}