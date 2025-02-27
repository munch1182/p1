package com.munch1182.lib

import android.util.Log

object LogLog {
    fun log(vararg any: Any) {
        Log.d("LogLog", any.joinToString(" "))
    }
}