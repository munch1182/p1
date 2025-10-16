package com.munch1182.p1

import android.app.Activity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.munch1182.lib.base.startActivity
import com.munch1182.p1.base.BaseActivity
import com.munch1182.p1.ui.ClickButton
import com.munch1182.p1.ui.RvPage
import com.munch1182.p1.ui.setContentWithScroll
import com.munch1182.p1.views.AboutActivity
import com.munch1182.p1.views.BluetoothActivity
import com.munch1182.p1.views.NetPhoneActivity
import com.munch1182.p1.views.ResultActivity
import com.munch1182.p1.views.ScanActivity
import com.munch1182.p1.views.StartIndexVM

class MainActivity : BaseActivity() {

    private val startIndexVM by viewModels<StartIndexVM>()

    companion object {
        val items: Array<Pair<Any, Activity.() -> Unit>> by lazy {
            arrayOf(
                newClass<ResultActivity>("权限相关"),
                newClass<ScanActivity>("扫码相关"),
                newClass<BluetoothActivity>("蓝牙相关"),
                newClass<NetPhoneActivity>("网络电话"),
                newClass<AboutActivity>("关于")
            )
        }

        inline fun <reified ACT : Activity> newClass(name: String) = Pair<Any, Activity.() -> Unit>(name) { startActivity<ACT>() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContentWithScroll {
            RvPage(items) {
                val first = it.first
                val text = first as? String ?: (first as? Int)?.let { i -> getString(i) } ?: first.toString()
                ClickButton(text) { it.second.invoke(this@MainActivity) }
            }
        }
        startIndexVM.startIndex.observe(this) { index ->
            if (index != null && index != 0) {
                items.getOrNull(index - 1)?.second?.invoke(this)
            }
        }
    }
}

