package com.munch1182.p1.base

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.view.accessibility.AccessibilityEvent
import com.munch1182.core.android.AppHelper
import com.munch1182.core.android.Log
import com.munch1182.core.android.click
import com.munch1182.core.android.findByScrollOrNull
import com.munch1182.core.common.launchMain
import kotlinx.coroutines.delay

/**
 * 无障碍服务;
 *
 * 需要先注册:
 * ```XML
 * <service
 * android:name=".base.AppAccessibilityService"
 *  android:exported="false"
 *  android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
 *   tools:ignore="AccessibilityPolicy">
 *   <intent-filter>
 *   <action android:name="android.accessibilityservice.AccessibilityService" />
 *   </intent-filter>
 *    <meta-data
 *     android:name="android.accessibilityservice"
 *     android:resource="@xml/accessibility_service_config" />
 * </service>
 * ```
 *
 * res/xml/accessibility_service_config.xml:
 * ```XML
 * <?xml version="1.0" encoding="utf-8"?>
 * <accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
 *     android:description="@string/accessibility_service_description"
 *     android:accessibilityEventTypes="typeAllMask"
 *     android:accessibilityFeedbackType="feedbackGeneric"
 *     android:accessibilityFlags="flagDefault"
 *     android:canRetrieveWindowContent="true"
 *     android:notificationTimeout="100"
 *     android:packageNames="com.android.settings"  />
 * ```
 */
@SuppressLint("AccessibilityPolicy")
class AppAccessibilityService : AccessibilityService() {
    companion object {
        private const val TAG = "AppAccessibilityService"
        private const val TARGET_PKG_ADB_WIFI = "com.android.settings"

        private const val CLASS_NAME_DEVELOPMENT_SETTINGS = $$"com.android.settings.Settings$DevelopmentSettingsDashboardActivity"

        private const val CLASS_NAME_SUB_SETTINGS = "com.android.settings.SubSettings"
    }

    override fun onAccessibilityEvent(p0: AccessibilityEvent?) {
        if (p0 != null //
            && p0.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED //
            && p0.packageName.toString() == TARGET_PKG_ADB_WIFI //
        ) {

            rootInActiveWindow ?: return
            val className = p0.className
            Log.d(TAG, "className: $className")

            AppHelper.launchMain {
                when (className) {
                    CLASS_NAME_DEVELOPMENT_SETTINGS -> {
                        val adbWifiNode = findByScrollOrNull("无线调试")
                        Log.d(TAG, "adbWifiNode: $adbWifiNode")
                        if (adbWifiNode != null) delay(450)
                        adbWifiNode?.click(this@AppAccessibilityService)
                    }

                    CLASS_NAME_SUB_SETTINGS -> {

                    }
                }

            }
        }
    }


    override fun onInterrupt() {
    }
}