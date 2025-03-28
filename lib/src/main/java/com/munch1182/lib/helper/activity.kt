package com.munch1182.lib.helper

import android.app.Activity
import android.app.Application
import com.munch1182.lib.AppHelper
import com.munch1182.lib.DefaultActivityLifecycleCallbacks
import com.munch1182.lib.base.log
import java.lang.ref.WeakReference
import java.util.Stack


object ActivityCurrHelper {

    private val stack = Stack<WeakReference<Activity>>()
    private val log = this.log(false)

    val curr: Activity?
        get() = stack.getOrNull(stack.lastIndex)?.get()

    val appInFocus: Boolean
        get() = stack.isNotEmpty()

    private val callback by lazy {
        object : DefaultActivityLifecycleCallbacks() {
            override fun onActivityStarted(activity: Activity) {
                super.onActivityStarted(activity)
                stack.push(WeakReference(activity))
                log.logStr("put $activity")
            }

            override fun onActivityStopped(activity: Activity) {
                super.onActivityStopped(activity)
                val remove = stack.removeFirstOrNull()
                log.logStr("remove ${remove?.get()}")
            }
        }
    }

    private val app by lazy { AppHelper.baseContext as Application }

    fun register() {
        app.registerActivityLifecycleCallbacks(callback)
    }

    fun unregister() {
        app.unregisterActivityLifecycleCallbacks(callback)
    }
}