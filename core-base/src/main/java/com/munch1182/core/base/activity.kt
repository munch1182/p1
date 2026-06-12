package com.munch1182.core.base

import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity : AppCompatActivity() {

    private val screenName get() = this.javaClass.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        AppAnalytics.trackScreen(screenName, mapOf(newStateProp("onCreate")))
    }

    override fun onResume() {
        super.onResume()
        AppAnalytics.trackScreen(screenName, mapOf(newStateProp("onResume")))
    }

    override fun onPause() {
        super.onPause()
        AppAnalytics.trackScreen(screenName, mapOf(newStateProp("onPause")))
    }

    override fun recreate() {
        super.recreate()
        AppAnalytics.trackScreen(screenName, mapOf(newStateProp("recreate")))
    }

    override fun onDestroy() {
        super.onDestroy()
        AppAnalytics.trackScreen(screenName, mapOf(newStateProp("onDestroy")))
    }

    private fun newStateProp(str: String) = "state" to str
}
