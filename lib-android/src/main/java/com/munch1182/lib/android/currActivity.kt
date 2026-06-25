package com.munch1182.lib.android

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.munch1182.lib.android.ActivityCurrHelper.register
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.ref.WeakReference

/**
 * 提供一个在[Activity.onResume]到[Activity.onPause]之间可用的[Activity]对象, 其余生命周期则为null;
 * 提供app前台/后台监听
 * 需要手动注册
 *
 * @see register
 */
object ActivityCurrHelper {
    private const val TAG = "ActivityCurrHelper"

    /**
     * 当前已经[Activity.onResume]还没[Activity.onPause]之间的[Activity]对象
     */
    val curr
        get() = currentResumedActivity?.get()?.takeIf { !it.isFinishing && !it.isDestroyed }

    private val _isAppForeground = MutableStateFlow(false)

    /**
     * 提供app是否在前台的状态
     */
    val isAppForeground: StateFlow<Boolean> = _isAppForeground.asStateFlow()

    @Volatile
    private var currentResumedActivity: WeakReference<Activity>? = null
    private var resumedCount = 0

    private val callback = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(p0: Activity, p1: Bundle?) {
        }

        override fun onActivityDestroyed(p0: Activity) {
            if (currentResumedActivity?.get() == p0) {
                currentResumedActivity = null
            }
        }

        override fun onActivityPaused(p0: Activity) {
            currentResumedActivity = null
            resumedCount--
            if (resumedCount == 0) _isAppForeground.value = false
            Log.d(TAG, "onActivityPaused: curr: ${currentResumedActivity?.get()}: ${_isAppForeground.value}")
        }

        override fun onActivityResumed(p0: Activity) {
            currentResumedActivity = WeakReference(p0)
            if (resumedCount == 0) _isAppForeground.value = true
            resumedCount++
            Log.d(TAG, "onActivityResumed: curr: ${currentResumedActivity?.get()}: ${_isAppForeground.value}")
        }

        override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {
        }

        override fun onActivityStarted(p0: Activity) {
        }

        override fun onActivityStopped(p0: Activity) {
        }
    }

    fun register(app: Application = AppHelper.app) {
        Log.d(TAG, "registerActivityLifecycleCallbacks")
        app.registerActivityLifecycleCallbacks(callback)
    }

    fun unregister(app: Application = AppHelper.app) {
        Log.d(TAG, "unregisterActivityLifecycleCallbacks")
        app.unregisterActivityLifecycleCallbacks(callback)
    }
}