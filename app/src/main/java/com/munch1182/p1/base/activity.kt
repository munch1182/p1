package com.munch1182.p1.base

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * 基础活动类
 * 提供通用功能和配置，方便其他Activity继承和扩展
 *
 * 先预留实现
 */
open class BaseActivity : AppCompatActivity() {

    private val screenName get() = this.javaClass.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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
