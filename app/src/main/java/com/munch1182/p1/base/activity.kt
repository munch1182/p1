package com.munch1182.p1.base

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.munch1182.core.android.ActivityCurrHelper

/**
 * 获取当前Activity，
 * 只有在[android.app.Activity.onResume]和[android.app.Activity.onPause]直接有效，其它时候会抛出异常
 * 在非ui上下文需要慎重处理
 */
val currOrThrow get() = ActivityCurrHelper.curr ?: error("use context but ActivityCurrHelper.curr is null")

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
