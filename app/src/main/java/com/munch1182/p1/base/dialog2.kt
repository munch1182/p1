package com.munch1182.p1.base

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import com.munch1182.lib.base.appSetting
import com.munch1182.lib.base.statusHeight
import com.munch1182.lib.helper.result.ContactResultHelper
import com.munch1182.lib.helper.result.ContractDialogProvider
import com.munch1182.lib.helper.result.ContractTarget
import com.munch1182.lib.helper.result.PermissionDialogProvider
import com.munch1182.lib.helper.result.PermissionTarget
import com.munch1182.lib.helper.result.PermissionWithDialogProvider
import com.munch1182.lib.helper.result.ResultHelper
import com.munch1182.p1.ui.theme.PagePaddingModifier
import com.munch1182.p1.ui.theme.TextSize
import com.munch1182.p1.ui.theme.TextSm

fun <I, O> ResultHelper.ContractResultHelper<I, O>.onDialog(beforeMsg: String, afterMsg: String? = null, title: String = "注意") = onDialog {
    when (it) {
        ContractTarget.AfterRequest -> afterMsg?.let { DialogHelper.newMessage(title, afterMsg) }
        ContractTarget.BeforeRequest -> DialogHelper.newMessage(title, beforeMsg)
    }
}

fun ResultHelper.PermissionResultHelper.onDialog(name: String, usage: String) =
    onDialog(providerPermissionDialog(name, usage))
        .onDialog(providerPermissionWithDialog(name, usage))

fun ContactResultHelper.PermissionPartResultHelper.onDialog(name: String, usage: String) =
    onDialog(providerPermissionDialog(name, usage))
        .onDialog(providerPermissionWithDialog(name, usage))

fun providerPermissionDialog(name: String, usage: String) = PermissionDialogProvider { target, _ ->
    when (target) {
        PermissionTarget.ForRequestFirst -> null
        PermissionTarget.ForRequestDenied -> DialogHelper.newMessage("权限请求", "请允许应用使用${name}权限，否则${usage}功能将无法使用", "授权")
        PermissionTarget.ForRequestNeverAsk -> DialogHelper.newMessage("权限拒绝", "可前往设置界面手动授予${name}权限", "前往")
    }
}

fun providerPermissionWithDialog(name: String, usage: String) = PermissionWithDialogProvider { target ->
    DialogHelper.newTopNotice {
        Column(Modifier.padding(top = with(LocalDensity.current) { statusHeight().toDp() })) {
            Card(PagePaddingModifier.fillMaxWidth()) {
                Column(PagePaddingModifier) {
                    Text(name, color = Color.Black, fontSize = TextSize)
                    Text("${name}权限用于$usage", color = Color.DarkGray, fontSize = TextSm)
                }
            }
        }
    }
}

fun ResultHelper.PermissionResultHelper.appIntent() = onIntent(appSetting())
fun ContactResultHelper.PermissionPartResultHelper.appIntent() = onIntent(appSetting())

fun providerContractDialog(name: String, msgBefore: String, msgAfter: String? = null, ok: String = ctx!!.getString(android.R.string.ok)): ContractDialogProvider = ContractDialogProvider {
    when (it) {
        ContractTarget.AfterRequest -> msgAfter?.let { it -> DialogHelper.newMessage(name, it, ok) }
        ContractTarget.BeforeRequest -> DialogHelper.newMessage(name, msgBefore, ok)
    }
}

fun ResultHelper.JudgeResultHelper.onDialog(
    name: String, msgBefore: String, msgAfter: String? = null, ok: String = ctx!!.getString(android.R.string.ok)
) = onDialog(providerContractDialog(name, msgBefore, msgAfter, ok))

fun ContactResultHelper.JudgePartResultHelper.onDialog(
    name: String, msgBefore: String, msgAfter: String? = null, ok: String = ctx!!.getString(android.R.string.ok)
) = onDialog(providerContractDialog(name, msgBefore, msgAfter, ok))