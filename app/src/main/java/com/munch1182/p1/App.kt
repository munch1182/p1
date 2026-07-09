package com.munch1182.p1

import android.app.Application
import android.content.Context
import com.munch1182.core.base.CoreInit
import com.munch1182.lib.android.AppHelper
import com.munch1182.lib.android.CompositeLogger
import com.munch1182.lib.android.ConsoleLogger
import com.munch1182.lib.android.Log
import com.munch1182.lib.android.isDebug
import com.munch1182.lib.common.Logger
import com.munch1182.lib.common.Timer
import com.munch1182.lib.common.launchIO
import com.tencent.bugly.crashreport.CrashReport
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App : Application() {

    internal var appLauncherTimer: Timer? = null

    private companion object {
        private const val TAG_APP_LAUNCHER = "AppLauncherTimer"
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        appLauncherTimer = Timer(
            TAG_APP_LAUNCHER, ConsoleLogger( // 因为先于Log的初始化
                isDebug, listOf(
                    Logger::class, Log::class, CompositeLogger::class
                ).map { it.java.name }
            ), !isDebug
        )
        appLauncherTimer?.start()
    }

    /**
     * 结束录音
     */
    fun endTimer() {
        val cost = appLauncherTimer?.end() ?: return
        android.util.Log.d("Timer", "AppLauncher cost: $cost ms (add log)")
        appLauncherTimer = null
    }


    override fun onCreate() {
        super.onCreate()
        CoreInit.init(AppHelper)
        AppHelper.launchIO { initBuglyFirst() }
    }

    private fun initBuglyFirst() {
        CrashReport.initCrashReport(this@App, AppConstParam.BUGLY_APP_ID, AppHelper.isDebug)
    }

}

private object AppConstParam {
    const val BUGLY_APP_ID = "f8e4d8f9d1"
}
