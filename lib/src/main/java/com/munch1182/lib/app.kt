package com.munch1182.lib

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import androidx.startup.Initializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope

class LibContextInitializer : Initializer<AppHelper> {
    override fun create(context: Context): AppHelper {
        AppHelper.manualInit(context.applicationContext as Application)
        return AppHelper
    }

    override fun dependencies() = emptyList<Class<out Initializer<*>>>()
}


@OptIn(DelicateCoroutinesApi::class)
object AppHelper : ContextWrapper(null), CoroutineScope by GlobalScope {

    fun manualInit(app: Application) {
        attachBaseContext(app)
    }
}


open class DefaultActivityLifecycleCallbacks : Application.ActivityLifecycleCallbacks {
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