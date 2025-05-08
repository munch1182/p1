package com.munch1182.p1.views.libview

import android.os.Bundle
import androidx.compose.runtime.Composable
import com.munch1182.p1.base.BaseActivity
import com.munch1182.p1.ui.JumpButton
import com.munch1182.p1.ui.setContentWithRv

class ViewActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentWithRv { Click() }
    }

    @Composable
    private fun Click() {
        JumpButton("SwapMenuLayout", clazz = SwapMenuLayoutActivity::class)
        JumpButton("RecyclerView", clazz = RecyclerviewActivity::class)
        JumpButton("NumberHeightView", clazz = NumberHeightViewActivity::class)
    }
}