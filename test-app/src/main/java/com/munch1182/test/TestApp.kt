package com.munch1182.test

import android.app.Application
import com.munch1182.core.base.CoreInit
import com.munch1182.lib.android.AppHelper
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TestApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CoreInit.init(AppHelper)
    }
}
