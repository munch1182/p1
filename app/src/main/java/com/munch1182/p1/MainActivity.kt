package com.munch1182.p1

import android.os.Bundle
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

class MainActivity : BaseActivity() {

    private val items: Array<Pair<Any, () -> Unit>> by lazy {
        arrayOf(
            "权限相关" to { startActivity<ResultActivity>() },
            "蓝牙相关" to { startActivity<BluetoothActivity>() },
            "网络电话" to { startActivity<NetPhoneActivity>() },
            "关于" to { startActivity<AboutActivity>() }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentWithScroll {
            RvPage(items) {
                val first = it.first
                val text = first as? String ?: (first as? Int)?.let { i -> getString(i) } ?: first.toString()
                ClickButton(text) { it.second.invoke() }
            }
        }
    }
}

