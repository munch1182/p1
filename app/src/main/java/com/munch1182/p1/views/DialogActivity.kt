package com.munch1182.p1.views

import android.os.Bundle
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.munch1182.lib.base.toast
import com.munch1182.lib.helper.dialog.onResult
import com.munch1182.p1.base.BaseActivity
import com.munch1182.p1.base.DialogHelper
import com.munch1182.p1.ui.ClickButton
import com.munch1182.p1.ui.setContentWithRv
import com.munch1182.p1.ui.theme.PagePaddingModifier

class DialogActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentWithRv { Views() }
    }

    @Composable
    private fun Views() {
        ClickButton("MESSAGE") {
            DialogHelper.newMessage(msg = "123123").onResult { toast("chose: $it") }.show()
        }
        ClickButton("PROGRESS") {
            DialogHelper.newProgress("loading", false).showCancelDelay(3000L).onResult { toast("chose: $it") }.show()
        }
        ClickButton("BOTTOM") {
            DialogHelper.newBottom {
                Column(PagePaddingModifier) {
                    Text("新建View")
                    Text("111")
                    Text("222")
                    Text("333")
                }
            }.onResult { }.show()
        }
    }
}