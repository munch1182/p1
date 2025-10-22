package com.munch1182.p1.views

import android.Manifest
import android.annotation.SuppressLint
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.munch1182.lib.base.ReRunJob
import com.munch1182.lib.base.launchIO
import com.munch1182.lib.base.toast
import com.munch1182.lib.helper.AudioFocus
import com.munch1182.lib.helper.AudioFocusHelper
import com.munch1182.lib.helper.AudioHelper
import com.munch1182.lib.helper.AudioStreamHelper
import com.munch1182.lib.helper.FileHelper
import com.munch1182.lib.helper.FileWriteHelper
import com.munch1182.lib.helper.RecordHelper
import com.munch1182.lib.helper.onResult
import com.munch1182.lib.helper.result.isAllGranted
import com.munch1182.lib.helper.result.manualIntent
import com.munch1182.lib.helper.result.permission
import com.munch1182.lib.helper.toAudio
import com.munch1182.p1.App
import com.munch1182.p1.base.BaseActivity
import com.munch1182.p1.base.DialogHelper
import com.munch1182.p1.base.onPermission
import com.munch1182.p1.ui.ClickIcon
import com.munch1182.p1.ui.Items
import com.munch1182.p1.ui.RvPageIter
import com.munch1182.p1.ui.SpacerV
import com.munch1182.p1.ui.StateButton
import com.munch1182.p1.ui.setContentWithScroll
import com.munch1182.p1.ui.theme.PagePadding
import com.munch1182.p1.ui.theme.PagePaddingHalf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File

class AudioActivity : BaseActivity() {
    private val recordVM by viewModels<RecordVM>()
    private val playVM by viewModels<AudioPlayVM>()
    private val audioVM by viewModels<AudioVM>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentWithScroll { Views() }
    }

    @Composable
    private fun Views() {
        val audioUiState by audioVM.uiState.collectAsState()
        val recordUiState by recordVM.uiState.collectAsState()
        val file by remember(recordUiState.recordFile) { derivedStateOf { recordUiState.recordFile } }
        var isExpand by remember { mutableStateOf(false) }
        if (audioUiState.isErr) {
            toast("操作失败")
            audioVM.clearErr()
        }
        StateButton("取消焦点", "获取焦点", audioUiState.isFocus) {
            if (audioUiState.isFocus) audioVM.abandonFocus() else audioVM.requestFocus()
        }
        SpacerV()
        ClickText("输入设备：${if (audioUiState.input == -1) "默认" else audioVM.inputs[audioUiState.input].type.toAudiTypeStr()}", true) {
            showDevs(audioVM.inputs, true) { audioVM.selectInput(it) }
        }
        ClickText("输出设备：${if (audioUiState.output == -1) "默认" else audioVM.outputs[audioUiState.output].type.toAudiTypeStr()}", true) {
            showDevs(audioVM.outputs, false) { audioVM.selectOutput(it) }
        }

        SpacerV()

        Row {
            StateButton("结束录音", "开始录音", recordUiState.isRecording) {
                withPermission { if (recordUiState.isRecording) recordVM.stopRecord() else recordVM.startRecord() }
            }
            ClickIcon(Icons.Filled.ArrowDropDown, modifier = Modifier.rotate(if (isExpand) 0f else 180f)) {
                isExpand = !isExpand
                if (isExpand) showRecordParamDialog { isExpand = false }
            }
        }

        SpacerV()

        file?.let { Play(it) }
    }

    @Composable
    private fun Play(file: File) {
        val audioUiState by playVM.uiState.collectAsState()
        Text(file.name)
        StateButton("停止", "播放", audioUiState.isPlaying) {
            if (audioUiState.isPlaying) playVM.stop() else playVM.play(recordVM.newAudio, file)
        }
    }

    private fun showRecordParamDialog(onDismiss: (Boolean) -> Unit) {
        DialogHelper.newBottom {
            val uiState by recordVM.uiState.collectAsState()
            Items(modifier = Modifier.padding(vertical = PagePadding)) {
                ClickText("采样: ${uiState.params.sampleRateInHz}") { recordVM.updateParams(uiState.params.nextRate) }
                ClickText("声道: ${uiState.params.channelConfig.toInChannelStr()}") { recordVM.updateParams(uiState.params.nextChannel) }
                ClickText("编码: ${uiState.params.audioFormat.toEncodingStr()}") { recordVM.updateParams(uiState.params.newFormat) }
                ClickText("时间: ${uiState.params.time}") { recordVM.updateParams(uiState.params.nextTime) }
            }
        }.onResult(onDismiss).show()
    }

    private fun showDevs(devs: Array<out AudioDeviceInfo>, isIn: Boolean, on: (Int) -> Unit) {
        val txt = 10.sp
        DialogHelper.newBottom(-1) { i ->
            var select by remember { mutableIntStateOf(-1) }
            RvPageIter(devs) { index, dev ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            i.update(index)
                            select = index
                        }
                        .padding(PagePadding, PagePaddingHalf)) {
                    Text("${dev.type.toAudiTypeStr()}${if (select == index) "*" else ""}", fontWeight = FontWeight.ExtraBold)
                    Text(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) "(${dev.address}, ${dev.productName})" else "(${dev.productName})", fontSize = txt)
                    Text(if (dev.sampleRates.isEmpty() || dev.sampleRates.size == 9) "*" else dev.sampleRates.joinToString("/"), fontSize = txt)
                    Text(if (dev.channelMasks.isEmpty()) "*" else dev.channelMasks.joinToString { i -> if (isIn) i.toInChannelStr() else i.toOutChannelStr() }, fontSize = txt)
                    Text(if (dev.encodings.isEmpty()) "*" else dev.encodings.joinToString { i -> i.toEncodingStr() }, fontSize = txt)
                }
            }
        }.onResult(on).show()
    }

    @Composable
    private fun ClickText(text: String, noPadding: Boolean = false, onClick: () -> Unit) {
        Text(
            text, Modifier
                .clickable(onClick = onClick)
                .padding(if (noPadding) 0.dp else PagePadding)
        )
    }

    private fun withPermission(p: () -> Unit) {
        permission(Manifest.permission.RECORD_AUDIO).onPermission("麦克风" to "录制音频").manualIntent().request { if (it.isAllGranted()) p() }
    }

    private fun Int.toInChannelStr(): String {
        return when (this) {
            AudioFormat.CHANNEL_IN_MONO -> "CHANNEL_IN_MONO"
            AudioFormat.CHANNEL_IN_STEREO -> "CHANNEL_IN_STEREO"
            else -> this.toString()
        }
    }

    private fun Int.toOutChannelStr(): String {
        return when (this) {
            AudioFormat.CHANNEL_OUT_MONO -> "CHANNEL_OUT_MONO"
            AudioFormat.CHANNEL_OUT_STEREO -> "CHANNEL_OUT_STEREO"
            else -> this.toString()
        }
    }

    private fun Int.toEncodingStr(): String {
        return when (this) {
            AudioFormat.ENCODING_PCM_8BIT -> "ENCODING_PCM_8BIT"
            AudioFormat.ENCODING_PCM_16BIT -> "ENCODING_PCM_16BIT"
            AudioFormat.ENCODING_PCM_FLOAT -> "ENCODING_PCM_FLOAT"
            AudioFormat.ENCODING_AC3 -> "ENCODING_AC3"
            AudioFormat.ENCODING_E_AC3 -> "ENCODING_E_AC3"
            AudioFormat.ENCODING_DTS -> "ENCODING_DTS"
            AudioFormat.ENCODING_DTS_HD -> "ENCODING_DTS_HD"
            AudioFormat.ENCODING_IEC61937 -> "ENCODING_IEC61937"
            AudioFormat.ENCODING_PCM_32BIT -> "ENCODING_PCM_32BIT"
            else -> this.toString()
        }
    }

    private fun Int.toAudiTypeStr(): String {
        val name = when (this) {
            AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "BUILTIN_EARPIECE"
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "BUILTIN_SPEAKER"
            AudioDeviceInfo.TYPE_WIRED_HEADSET -> "WIRED_HEADSET"
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "WIRED_HEADPHONES"
            AudioDeviceInfo.TYPE_LINE_ANALOG -> "WIRED_HEADPHONES"
            AudioDeviceInfo.TYPE_LINE_DIGITAL -> "WIRED_HEADPHONES"
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "BLUETOOTH_SCO"
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "BLUETOOTH_A2DP"
            AudioDeviceInfo.TYPE_BUILTIN_MIC -> "BUILTIN_MIC"
            AudioDeviceInfo.TYPE_FM_TUNER -> "FM_TUNER"
            AudioDeviceInfo.TYPE_TELEPHONY -> "TELEPHONY"
            AudioDeviceInfo.TYPE_USB_HEADSET -> "USB_HEADSET"
            AudioDeviceInfo.TYPE_REMOTE_SUBMIX -> "REMOTE_SUBMIX"
            AudioDeviceInfo.TYPE_BLE_HEADSET -> "BLE_HEADSET"
            AudioDeviceInfo.TYPE_BLE_SPEAKER -> "TYPE_BLE_SPEAKER"
            28 -> "TYPE_ECHO_REFERENCE"
            else -> this.toString()
        }
        return "$name($this)"
    }

}

/**
 * 录音
 */
internal class RecordVM : ViewModel() {

    private var recordHelper = RecordHelper.recognition()
    private var write = FileWriteHelper()

    private val _uiState = MutableStateFlow(
        RecordUiState(
            params = RecordParam(recordHelper.sampleRate, recordHelper.channelConfig, recordHelper.audioFormat)
        )
    )
    val uiState = _uiState.asStateFlow()

    val newAudio get() = runCatching { recordHelper.toAudio() }.getOrNull()
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
        if (recordHelper.audioFormat != param.audioFormat || recordHelper.sampleRate != param.sampleRateInHz || recordHelper.channelConfig != param.channelConfig) {
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

/**
 * 播放
 */
internal class AudioPlayVM : ViewModel() {
    private val _uiState = MutableStateFlow(AudioPlayUiState())
    private var audio: AudioStreamHelper? = null

    private val reRunJob = ReRunJob()
    val uiState = _uiState.asStateFlow()

    override fun onCleared() {
        super.onCleared()
        audio?.release()
    }

    fun play(audio: AudioStreamHelper?, file: File) {
        audio ?: return
        stop()
        this.audio = audio
        viewModelScope.launchIO(reRunJob.newContext) {
            audio.prepare()
            _uiState.update { it.copy(isPlaying = true) }
            val buff = audio.newBuff
            file.inputStream().use {
                var read = runCatching { it.read(buff) }.getOrNull() ?: 0
                while (read > 0) {
                    audio.write(buff, 0, read)
                    read = runCatching { it.read(buff) }.getOrNull() ?: 0
                }
                audio.writeOver {}
            }
            _uiState.update { it.copy(isPlaying = false) }
        }
    }

    fun stop() {
        this.audio?.release()
        reRunJob.cancel()
    }
}

/**
 * 音频状态
 */
internal class AudioVM : ViewModel() {
    private var select = Pair(-1, -1)
    private val _uiState = MutableStateFlow(AudioUiState())
    val uiState = _uiState.asStateFlow()
    private val audioFocus = AudioFocusHelper()
    val inputs get() = AudioHelper.inputs
    val outputs get() = AudioHelper.outputs

    init {
        audioFocus.add { n ->
            _uiState.update { it.copy(isFocus = n, isErr = false) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioFocus.release()
    }

    fun requestFocus() {
        val res = audioFocus.requestAudioFocusNow(AudioFocus.GainTransient, App.instance.appHandler)
        _uiState.update { if (res) it.copy(isFocus = true) else it.copy(isErr = true) }
    }

    fun abandonFocus() {
        val res = audioFocus.abandonAudioFocus()
        _uiState.update { if (res) it.copy(isFocus = false) else it.copy(isErr = true) }
    }

    fun clearErr() {
        _uiState.update { it.copy(isErr = false) }
    }

    fun selectInput(it: Int) {
        if (it == -1) return
        select = Pair(it, select.second)
        _uiState.update { it.copy(input = select.first) }
    }

    fun selectOutput(it: Int) {
        if (it == -1) return
        select = Pair(select.first, it)
        _uiState.update { it.copy(output = select.second) }
    }
}

internal data class AudioUiState(
    val isFocus: Boolean = false, val isErr: Boolean = false, val input: Int = -1, val output: Int = -1
)


internal data class RecordUiState(
    val isRecording: Boolean = false, val params: RecordParam, val recordFile: File? = null
)

internal data class AudioPlayUiState(
    val isPlaying: Boolean = false
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

