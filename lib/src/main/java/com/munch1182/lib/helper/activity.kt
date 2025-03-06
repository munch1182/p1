package com.munch1182.lib.helper

import android.app.Activity
import android.app.Application
import androidx.fragment.app.FragmentActivity
import com.munch1182.lib.AppHelper
import com.munch1182.lib.DefaultActivityLifecycleCallbacks
import java.lang.ref.WeakReference
import java.util.Stack

object ActivityCurrHelper {

    private val stack = Stack<WeakReference<Activity>>()

    val curr: Activity?
        get() = stack.getOrNull(stack.lastIndex)?.get()

    val currAsFA: FragmentActivity?
        get() = curr as? FragmentActivity

    val appInFocus: Boolean
        get() = stack.isNotEmpty()

    private val callback by lazy {
        object : DefaultActivityLifecycleCallbacks() {
            override fun onActivityStarted(activity: Activity) {
                super.onActivityStarted(activity)
                stack.push(WeakReference(activity))
            }

            override fun onActivityStopped(activity: Activity) {
                super.onActivityStopped(activity)
                stack.removeFirstOrNull()
            }
        }
    }

    private val app by lazy { AppHelper.applicationContext as Application }

    fun register() {
        app.registerActivityLifecycleCallbacks(callback)
    }

    fun unregister() {
        app.unregisterActivityLifecycleCallbacks(callback)
    }
}