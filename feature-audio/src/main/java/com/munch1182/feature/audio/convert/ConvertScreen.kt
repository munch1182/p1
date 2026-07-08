package com.munch1182.feature.audio.convert

import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CommentBank
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.munch1182.core.ui.PrimaryButton
import com.munch1182.core.ui.SplitH
import com.munch1182.core.ui.SplitW
import com.munch1182.core.ui.currAsFragmentActivityOrThrow
import com.munch1182.core.ui.dialog.DialogFactory
import com.munch1182.core.ui.theme.Dimens
import com.munch1182.core.ui.theme.paddingItem
import com.munch1182.core.ui.theme.paddingPage
import com.munch1182.lib.android.result.request
import com.munch1182.lib.common.launchIO

@Composable
fun ConvertScreen(vm: ConvertViewModel = hiltViewModel()) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    Column(Modifier.paddingPage()) {
        UploadSection { vm.selectFile(it) }
        state.error?.let { error ->
            SplitH()
            Text(error.str, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        SplitH()
        ConfigSection(state.config, state.currentFormatState, vm::selectFormat, vm::updateParam)
        SplitH()
        CommandView(state.currentFormatState?.cmd ?: "")
        SplitH()
        ConvertButton(enabled = state.currentFormatState != null && state.currFile != null, onClick = { })
    }
}

@Composable
private fun ConvertButton(enabled: Boolean, onClick: () -> Unit) {
    PrimaryButton("开始转换", enabled = enabled, onClick = onClick, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun CommandView(cmd: String) {
    Card(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp), colors = CardDefaults.cardColors(containerColor = Color.Black)
    ) {
        Column(Modifier.paddingItem()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CommentBank, null, Modifier.width(12.dp), tint = Color.White)
                SplitW()
                Text("命令", style = MaterialTheme.typography.bodySmall, color = Color.White)
            }
            Text(cmd, style = MaterialTheme.typography.bodySmall, color = Color.White)
        }
    }
}

@Composable
private fun ConfigSection(
    config: ConvertConfig?, formatState: FormatState?, onFormatSelected: (ConvertFormat) -> Unit, onParamChange: (String, String) -> Unit
) {
    var isShow by remember { mutableStateOf(false) }
    Column {
        Text("输出配置")
        SplitH()

        Card(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Row(
                Modifier
                    .fillMaxSize()
                    .clickable(config?.formats?.isEmpty() != true) {
                        val items = config?.formats?.map { it.name } ?: listOf()
                        showSelectDialog(items) {
                            onFormatSelected(config?.formats?.get(it) ?: return@showSelectDialog)
                        }
                    }
                    .padding(horizontal = Dimens.PaddingPage), verticalAlignment = Alignment.CenterVertically
            ) {
                if (formatState?.format?.name != null) Icon(Icons.Default.Audiotrack, null, Modifier.size(24.dp))
                SplitW()
                Text(formatState?.format?.name ?: "未选择", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                if ((config?.formats?.size ?: 0) > 1) {
                    Icon(
                        if (isShow) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null, Modifier.size(24.dp)
                    )
                }
            }
        }

        val params = formatState?.params
        if (!params.isNullOrEmpty()) {
            SplitH()
            Card(Modifier.fillMaxWidth()) {
                params.forEach { (key, value) ->
                    ParamItem(
                        key = key, //
                        value = value, //
                        options = config?.params?.get(key) ?: emptyList(), //
                        onValueSelected = { newValue -> onParamChange(key, newValue) })
                }
            }
        }
    }
}

@Composable
private fun ParamItem(
    key: String, value: String, options: List<String>, onValueSelected: (String) -> Unit
) {
    var isShow by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clickable(enabled = options.size > 1) {
                isShow = !isShow
                showSelectDialog(options) {
                    isShow = !isShow
                    onValueSelected(options[it])
                }
            }
            .padding(horizontal = Dimens.PaddingPage),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(key, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium)
        if (options.size > 1) {
            Icon(
                if (isShow) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null, Modifier.size(24.dp)
            )
        }
    }
}

fun showSelectDialog(options: List<String>, function: (Int) -> Unit) {
    DialogFactory.newBottom(Unit) { control ->
        Column(Modifier.padding(vertical = Dimens.PaddingPage)) {
            options.forEachIndexed { idx, str ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable {
                            function(idx)
                            control.dismiss()
                        }
                        .paddingItem(),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(str)
                }
            }
        }
    }.show()
}

@Composable
private fun UploadSection(onSelect: (Uri) -> Unit) {
    val scope = rememberCoroutineScope()
    Card(
        Modifier
            .fillMaxWidth()
            .height(300.dp)
            .clickable { scope.launchIO { selectFile(onSelect) } }) {
        Column(
            modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Filled.UploadFile, null, Modifier.size(80.dp))
            SplitH()
            Text("点击选择音视频文件")
            Text(
                "或者将文件分享到此应用", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

private suspend fun selectFile(onSelect: (Uri) -> Unit) {
    val uri = currAsFragmentActivityOrThrow.request(ActivityResultContracts.GetContent(), "audio/*")
    uri?.let { onSelect(it) }
}
