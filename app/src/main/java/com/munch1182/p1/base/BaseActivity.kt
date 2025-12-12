package com.munch1182.p1.base

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentActivity
import androidx.viewbinding.ViewBinding
import com.munch1182.android.lib.base.keepScreenOn

open class BaseActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setup()
    }

    protected open fun setup() {
        enableEdgeToEdge()
        keepScreenOn()
    }

    /**
     * 适配状态栏和底部栏，不适用于compose
     *
     * @param main 主布局
     */
    protected open fun fitWindow(main: View, fitState: Boolean = false, fitNavigation: Boolean = true) {
        ViewCompat.setOnApplyWindowInsetsListener(main) { v, insets ->
            val statusBar = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val navigationBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            v.setPadding(0, if (fitState) statusBar.top else 0, 0, if (fitNavigation) navigationBar.bottom else 0)
            insets
        }
    }
}

fun <VB : ViewBinding> Activity.bind(inflater: (LayoutInflater) -> VB): Lazy<VB> {
    return lazy { inflater(layoutInflater).apply { setContentView(root) } }
}