package com.munch1182.lib.helper

import android.os.Handler
import android.os.HandlerThread
import com.munch1182.lib.base.log

object AppHandler : HandlerThread("AppHandler") {
    private val log = log()

    init {
        start()
        log.logStr("AppHandler init: start()")
    }

    val handler get() = Handler(looper)
}