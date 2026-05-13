package com.munch1182.core.android

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.WeakReference

/**
 * 提供一个在[Activity.onResume]到[Activity.onPause]之间可用的[Activity]对象, 其余生命周期则为null
 */
object ActivityCurrHelper {

    /**
     * 不为null则一定在前台
     */
    val curr
        get() = currentResumedActivity?.get()?.takeIf { !it.isFinishing && !it.isDestroyed }

    @Volatile
    private var currentResumedActivity: WeakReference<Activity>? = null


    init {
        AppHelper.app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(p0: Activity, p1: Bundle?) {
            }

            override fun onActivityDestroyed(p0: Activity) {
                if (currentResumedActivity?.get() == p0) {
                    currentResumedActivity = null
                }
            }

            override fun onActivityPaused(p0: Activity) {
                currentResumedActivity = null
            }

            override fun onActivityResumed(p0: Activity) {
                currentResumedActivity = WeakReference(p0)
            }

            override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {
            }

            override fun onActivityStarted(p0: Activity) {
            }

            override fun onActivityStopped(p0: Activity) {
            }
        })
    }
}