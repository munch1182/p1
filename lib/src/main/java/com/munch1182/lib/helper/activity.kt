package com.munch1182.lib.helper

import android.app.Activity
import android.app.Application
import com.munch1182.lib.DefaultActivityLifecycleCallbacks

object ActivityCurrHelper {
    fun register(app: Application) {
        app.registerActivityLifecycleCallbacks(object : DefaultActivityLifecycleCallbacks() {
            override fun onActivityResumed(activity: Activity) {
                super.onActivityResumed(activity)
            }

            override fun onActivityPaused(activity: Activity) {
                super.onActivityPaused(activity)
            }
        })
    }
}