package com.munch1182.p1.views

import android.os.Bundle
import androidx.compose.runtime.Composable
import com.munch1182.p1.base.BaseActivity
import com.munch1182.p1.ui.ClickButton
import com.munch1182.p1.ui.setContentWithRv

class TestActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentWithRv { Views() }
    }

    private fun test1() {}
    private fun test2() {}
    private fun test3() {}

    @Composable
    private fun Views() {
        ClickButton("test1") { test1() }
        ClickButton("test2") { test2() }
        ClickButton("test3") { test3() }
    }
}