package com.munch1182.lib

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import androidx.startup.Initializer
import com.munch1182.lib.helper.ActivityCurrHelper

/**
 * StartUp 初始化
 */
class LibContextInitializer : Initializer<AppHelper> {
    override fun create(context: Context): AppHelper {
        AppHelper.manualInit(context.applicationContext as Application)
        return AppHelper
    }

    override fun dependencies() = emptyList<Class<out Initializer<*>>>()
}


/**
 * AppHelper本身代表这个[Application]的上下文
 *
 * 调用顺序：
 * [Application.attachBaseContext]
 * [AppHelper.manualInit]
 * [Application.onCreate]
 */
object AppHelper : ContextWrapper(null) {

    fun manualInit(app: Application) {
        attachBaseContext(app)
        ActivityCurrHelper.register(app)
    }
}