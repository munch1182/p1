package com.munch1182.p1

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import com.munch1182.lib.DefaultActivityLifecycleCallbacks
import com.munch1182.lib.base.LoggerGlobal
import com.munch1182.lib.base.Loglog
import com.munch1182.lib.base.keepScreenOn
import com.munch1182.lib.helper.ActivityCurrHelper
import com.munch1182.p1.measure.MeasureHelper

class App : Application() {

    companion object {
        lateinit var curr: App
    }

    override fun onCreate() {
        super.onCreate()
        curr = this
        MeasureHelper.setLog(Loglog)
        MeasureHelper.measureStart()
        LoggerGlobal.apply { prefix = "loglog" }
        ActivityCurrHelper.register()
        registerActivityLifecycleCallbacks(BaseActivitySetup())
        MeasureHelper.measureNow("App create")
    }
}

class BaseActivitySetup : DefaultActivityLifecycleCallbacks() {
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        super.onActivityCreated(activity, savedInstanceState)
        activity.keepScreenOn()
        if (activity is ComponentActivity) activity.enableEdgeToEdge()
    }
}
