package com.munch1182.p1.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.munch1182.core.android.awaitResult
import com.munch1182.core.common.launchMain
import com.munch1182.p1.dialog.Dialog
import com.munch1182.p1.dialog.Notice
import com.munch1182.p1.ui.PrimaryButton
import com.munch1182.p1.ui.theme.Dimens
import com.munch1182.p1.ui.theme.paddingPage
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph

@Destination<RootGraph>
@Composable
fun DialogScreen() {
    val scope = rememberCoroutineScope()
    var str by remember { mutableStateOf("") }
    val reset = { str = "" }
    Column(modifier = Modifier.paddingPage(), verticalArrangement = Arrangement.spacedBy(Dimens.PaddingItem)) {
        PrimaryButton("提示") {
            reset()
            Notice.toast("提示, 使用后台context")
        }
        PrimaryButton("提示弹窗") {
            reset()
            scope.launchMain {
                val chose = Dialog.newYesNoDialog("选择是或者否", ok = "是", cancel = "否").awaitResult()
                str = "选择了$chose"
            }
        }
        Text(str)
    }
}