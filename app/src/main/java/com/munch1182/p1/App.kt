package com.munch1182.p1

import android.app.Application
import android.os.Handler
import com.munch1182.lib.base.ThreadHelper

class App : Application() {
    lateinit var appHandler: Handler
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        appHandler = ThreadHelper.newHandler()
    }

    companion object {
        lateinit var instance: App
    }
}
