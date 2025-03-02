package com.munch1182.lib

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import androidx.startup.Initializer

class LibContextInitializer : Initializer<AppHelper> {
    override fun create(context: Context): AppHelper {
        AppHelper.manualInit(context.applicationContext as Application)
        return AppHelper
    }

    override fun dependencies() = emptyList<Class<out Initializer<*>>>()
}


object AppHelper : ContextWrapper(null) {

    fun manualInit(app: Application) {
        attachBaseContext(app)
    }
}