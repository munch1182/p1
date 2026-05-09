package com.munch1182.p1

import android.app.Application
import com.munch1182.core.android.AppHelper
import com.munch1182.core.android.Log
import com.munch1182.core.android.initDefault
import com.tencent.bugly.crashreport.CrashReport

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.initDefault()
        initBuglyFirst()
    }
    
    /**
     * 作为初始化的特例，在主线程初始化
     */
    private fun initBuglyFirst() {
        // https://bugly.qq.com/docs/user-guide/instruction-manual-android/?v=1.0.0
        CrashReport.initCrashReport(this@App, AppConstParam.BUGLY_APP_ID, AppHelper.isDebug)
    }
}


private object AppConstParam {
    const val BUGLY_APP_ID = "f8e4d8f9d1"
}
