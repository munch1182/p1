package com.munch1182.feature.audio.record

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.munch1182.core.ui.KeyValueSelectItem
import com.munch1182.core.ui.RunningStateButton
import com.munch1182.core.ui.showSelectDialog
import com.munch1182.core.ui.theme.Dimens
import com.munch1182.core.ui.theme.paddingPage

@Composable
internal fun RecordScreen(vm: RecordViewModel = hiltViewModel()) {

    var isShowIdx by remember { mutableIntStateOf(-1) }
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    Column(Modifier.paddingPage(), verticalArrangement = Arrangement.spacedBy(Dimens.PaddingItem)) {
        KeyValueSelectItem("采样率", uiState.cfg.sampleRate.toString(), isShowIdx == 0, modifier = Modifier.fillMaxWidth()) {
            val opts = listOf("16000", "44100")
            isShowIdx = 0
            showSelectDialog(opts) { idx ->
                isShowIdx = -1
                vm.updateConfig(uiState.cfg.copy(sampleRate = opts[idx].toInt()))
            }
        }
        val channelCountStr = when (uiState.cfg.channelCount) {
            2 -> "双声道"
            else -> "单声道"
        }
        KeyValueSelectItem("声道", channelCountStr, isShowIdx == 1, modifier = Modifier.fillMaxWidth()) {
            val opts = listOf("单声道", "双声道")
            isShowIdx = 0
            showSelectDialog(opts) { idx ->
                isShowIdx = -1
                vm.updateConfig(uiState.cfg.copy(channelCount = idx + 1))
            }
        }
        KeyValueSelectItem("采样位深", "${uiState.cfg.audioFormat * 4}bit", isShowIdx == 2, modifier = Modifier.fillMaxWidth()) {
            val opts = listOf("8", "16")
            isShowIdx = 0
            showSelectDialog(opts.map { "${it}bit" }) { idx ->
                isShowIdx = -1
                vm.updateConfig(uiState.cfg.copy(audioFormat = opts[idx].toInt()))
            }
        }

        RunningStateButton(
            uiState.isRecording, text = if (uiState.isRecording) "停止录音" else "开始录音"
        ) { vm.toggleRecord() }
    }
}