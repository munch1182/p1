package com.munch1182.p1.views

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.munch1182.android.lib.AppHelper
import com.munch1182.android.lib.base.Loglog
import com.munch1182.android.lib.base.launchIO
import com.munch1182.android.lib.base.selectFile
import com.munch1182.android.lib.helper.result.intent
import com.munch1182.android.net.gson
import com.munch1182.android.net.stringCatch
import com.munch1182.p1.base.DataHelper
import com.munch1182.p1.base.DialogHelper
import com.munch1182.p1.base.onDialog
import com.munch1182.p1.ui.ClickButton
import com.munch1182.p1.ui.Items
import com.munch1182.p1.ui.SpacerV
import com.munch1182.p1.ui.corner
import com.munch1182.p1.ui.theme.PagePaddingHalf
import com.munch1182.p1.ui.theme.PagePaddingModifier
import com.munch1182.p1.ui.theme.TextSm
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@Composable
fun ConfigView(vm: ConfigVM = viewModel()) {
    Items(Modifier.fillMaxWidth()) {
        val state by vm.uiState.collectAsState()

        ConfigItem("加载Translate配置", state.transConfigState, state.trans != null, { vm.openTransConfig(it) }) {
            state.trans?.let { showTransConfig(it) }
        }

        SpacerV()
    }
}

@Composable
private fun ConfigItem(title: String, state: String, enabled: Boolean, select: (Uri) -> Unit, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ClickButton(title) {
            intent(selectFile("application/json").apply { addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) })
                .onDialog("选择配置文件(json)来读取并设置")
                .request {
                    val uri = it?.data?.data
                    if (uri == null) return@request
                    select.invoke(uri)
                }
        }
        Text(
            state, fontSize = TextSm, modifier = Modifier
                .corner()
                .clickable(enabled, onClick = onClick)
                .padding(PagePaddingHalf)

        )
    }
}

private fun showTransConfig(trans: DataHelper.Config.Translate.Data) {
    Loglog.log(trans)
    DialogHelper.newBottom {
        Items(PagePaddingModifier) {
            Text(gson.stringCatch(trans) ?: "配置文件读取失败")
        }
    }.show()
}

@Stable
class ConfigVM : ViewModel() {
    private val _uiState = MutableStateFlow(ConfigUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launchIO {
            val data = DataHelper.Config.Translate.get()
            _uiState.value = _uiState.value.copy(transConfigState = if (data != null) "已配置" else "", trans = data)
        }
    }

    fun openTransConfig(uri: Uri) {
        _uiState.value = _uiState.value.copy(transConfigState = "加载中")
        viewModelScope.launchIO {
            val str = AppHelper.contentResolver.openInputStream(uri)?.let { String(it.readBytes()) }
            if (str != null) {
                DataHelper.Config.Translate.save(str)
                val data = DataHelper.Config.Translate.get()
                _uiState.value = _uiState.value.copy(transConfigState = "加载成功", trans = data)
                return@launchIO
            }
            _uiState.value = _uiState.value.copy(transConfigState = "加载失败")
        }
    }
}

@Stable
data class ConfigUiState(val transConfigState: String = "", val trans: DataHelper.Config.Translate.Data? = null)