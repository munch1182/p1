package com.munch1182.p1.views

import android.Manifest
import android.annotation.SuppressLint
import android.media.AudioFormat
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.munch1182.lib.base.ReRunJob
import com.munch1182.lib.base.launchIO
import com.munch1182.lib.helper.FileHelper
import com.munch1182.lib.helper.FileWriteHelper
import com.munch1182.lib.helper.RecordHelper
import com.munch1182.lib.helper.onResult
import com.munch1182.lib.helper.result.isAllGranted
import com.munch1182.lib.helper.result.manualIntent
import com.munch1182.lib.helper.result.permission
import com.munch1182.p1.base.BaseActivity
import com.munch1182.p1.base.DialogHelper
import com.munch1182.p1.base.onPermission
import com.munch1182.p1.ui.ClickButton
import com.munch1182.p1.ui.ClickIcon
import com.munch1182.p1.ui.Items
import com.munch1182.p1.ui.SpacerV
import com.munch1182.p1.ui.StateButton
import com.munch1182.p1.ui.setContentWithScroll
import com.munch1182.p1.ui.theme.PagePadding
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File

class AudioActivity : BaseActivity() {
    private val vm by viewModels<AudioVM>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentWithScroll { Views() }
    }

    @Composable
    private fun Views() {
        val uiState by vm.uiState.collectAsState()
        var isExpand by remember { mutableStateOf(false) }
        Row {
            StateButton("结束录音", "开始录音", uiState.isRecording) {
                withPermission { if (uiState.isRecording) vm.stopRecord() else vm.startRecord() }
            }
            ClickIcon(Icons.Filled.ArrowDropDown, modifier = Modifier.rotate(if (isExpand) 0f else 180f)) {
                isExpand = !isExpand
                if (isExpand) showRecordParamDialog { isExpand = false }
            }
        }
        
        SpacerV()

        if (uiState.recordFile != null) {
            Text(uiState.recordFile?.name ?: "")
            ClickButton("播放") {}
        }
    }

    private fun showRecordParamDialog(onDismiss: (Boolean) -> Unit) {
        DialogHelper.newBottom {
            val uiState by vm.uiState.collectAsState()
            Items(modifier = Modifier.padding(vertical = PagePadding)) {
                Text(
                    "采样率: ${uiState.params.sampleRateInHz}", modifier = Modifier
                        .clickable { vm.updateParams(uiState.params.nextRate) }
                        .padding(PagePadding))
                Text(
                    "声道: ${uiState.params.channelConfig}", modifier = Modifier
                        .clickable { vm.updateParams(uiState.params.nextChannel) }
                        .padding(PagePadding))
                Text(
                    "编码格式: ${uiState.params.audioFormat}", modifier = Modifier
                        .clickable { vm.updateParams(uiState.params.newFormat) }
                        .padding(PagePadding))
                Text(
                    "间隔时间: ${uiState.params.time}", modifier = Modifier
                        .clickable { vm.updateParams(uiState.params.nextTime) }
                        .padding(PagePadding))
            }
        }.onResult(onDismiss).show()
    }

    private fun withPermission(p: () -> Unit) {
        permission(Manifest.permission.RECORD_AUDIO).onPermission("麦克风" to "录制音频").manualIntent().request { if (it.isAllGranted()) p() }
    }
}

internal class AudioVM : ViewModel() {

    private var recordHelper = RecordHelper.recognition()
    private var write = FileWriteHelper()

    private val _uiState = MutableStateFlow(
        AudioUiState(
            params = RecordParam(recordHelper.sampleRateInHz, recordHelper.channelConfig, recordHelper.audioFormat)
        )
    )
    val uiState = _uiState.asStateFlow()
    private val recordJob = ReRunJob()

    override fun onCleared() {
        super.onCleared()
        stopRecord()
        recordHelper.release()
        write.complete()
    }

    @SuppressLint("MissingPermission")
    fun startRecord() {
        _uiState.update { it.copy(isRecording = true, recordFile = null) }
        checkRecordParam2Update()
        write.prepare(FileHelper.newCache("audio", "record.pcm"), true)
        viewModelScope.launchIO(recordJob.newContext) {
            recordHelper.record(_uiState.value.params.time).collect { write.write(it) }
        }
    }

    private fun checkRecordParam2Update() {
        val param = _uiState.value.params
        if (recordHelper.audioFormat != param.audioFormat || recordHelper.sampleRateInHz != param.sampleRateInHz || recordHelper.channelConfig != param.channelConfig) {
            recordHelper.release()
            recordHelper = RecordHelper(param.sampleRateInHz, param.channelConfig, param.audioFormat)
        }
    }

    fun stopRecord() {
        recordJob.cancel()
        val file = write.complete()
        _uiState.update { it.copy(isRecording = false, recordFile = file) }
    }

    fun updateParams(params: RecordParam) {
        _uiState.update { it.copy(params = params) }
    }
}

internal data class AudioUiState(
    val isRecording: Boolean = false, val params: RecordParam, val recordFile: File? = null
)

internal data class RecordParam(
    val sampleRateInHz: Int, val channelConfig: Int, val audioFormat: Int, val time: Long = 40L
) {
    val nextRate
        get() = this.copy(
            sampleRateInHz = when (sampleRateInHz) {
                8000 -> 16000
                16000 -> 22050
                22050 -> 44100
                44100 -> 8000
                else -> 8000
            }
        )

    val nextChannel
        get() = this.copy(
            channelConfig = when (channelConfig) {
                AudioFormat.CHANNEL_IN_MONO -> AudioFormat.CHANNEL_IN_STEREO
                AudioFormat.CHANNEL_IN_STEREO -> AudioFormat.CHANNEL_IN_MONO
                else -> AudioFormat.CHANNEL_IN_MONO
            }
        )

    val newFormat
        get() = this.copy(
            audioFormat = when (audioFormat) {
                AudioFormat.ENCODING_PCM_16BIT -> AudioFormat.ENCODING_PCM_8BIT
                else -> AudioFormat.ENCODING_PCM_16BIT
            }
        )

    val nextTime
        get() = this.copy(
            time = when (time) {
                40L -> 100
                100L -> 200
                else -> 40
            }
        )
}

