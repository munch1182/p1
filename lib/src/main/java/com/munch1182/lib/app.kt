package com.munch1182.lib

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.Bundle
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.startup.Initializer

class LibContextInitializer : Initializer<AppHelper> {
    override fun create(context: Context): AppHelper {
        AppHelper.manualInit(context.applicationContext as Application)
        return AppHelper
    }

    override fun dependencies() = emptyList<Class<out Initializer<*>>>()
}


object AppHelper : ContextWrapper(null) {

    internal fun manualInit(app: Application) {
        attachBaseContext(app)
    }
}

open class ActivityLifecycleSimpleCallbacks : ActivityLifecycleCallbacks {
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
    }

    override fun onActivityStarted(activity: Activity) {
    }

    override fun onActivityResumed(activity: Activity) {
    }

    override fun onActivityPaused(activity: Activity) {
    }

    override fun onActivityStopped(activity: Activity) {
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    override fun onActivityDestroyed(activity: Activity) {
    }
}

@ChecksSdkIntAtLeast(parameter = 0)
fun checkSDK(sdkInt: Int) = Build.VERSION.SDK_INT >= sdkInt