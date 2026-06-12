package com.munch1182.core.base

import com.munch1182.lib.android.ActivityCurrHelper
import com.munch1182.lib.android.AppHelper
import com.munch1182.lib.android.Log
import com.munch1182.lib.android.enableStrictMode
import com.munch1182.lib.android.initDefault
import com.munch1182.lib.common.launchIO

object CoreInit {
    fun init(application: AppHelper) {
        if (AppHelper.isDebug) enableStrictMode()
        application.launchIO { Log.initDefault() }
        ActivityCurrHelper.register()
    }
}
