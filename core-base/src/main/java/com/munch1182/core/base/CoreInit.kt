package com.munch1182.core.base

import com.munch1182.lib.android.ActivityCurrHelper
import com.munch1182.lib.android.AppHelper
import com.munch1182.lib.android.enableStrictMode
import com.munch1182.lib.android.initDefaultLogger
import com.munch1182.lib.common.launchIO

object CoreInit {
    fun init(application: AppHelper) {
        if (AppHelper.isDebug) enableStrictMode()
        application.launchIO { initDefaultLogger() }
        ActivityCurrHelper.register()
    }
}
