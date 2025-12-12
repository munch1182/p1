package com.munch1182.p1.views

import android.Manifest
import android.annotation.SuppressLint
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ForkLeft
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.munch1182.android.lib.base.ReRunJob
import com.munch1182.android.lib.base.formatFileSize
import com.munch1182.android.lib.base.launchIO
import com.munch1182.android.lib.base.withIO
import com.munch1182.android.lib.base.getAudioDuration
import com.munch1182.android.lib.base.getPcmDuration
import com.munch1182.android.lib.base.toDurationStrSmart
import com.munch1182.android.lib.helper.AudioStreamHelper
import com.munch1182.android.lib.helper.FileHelper
import com.munch1182.android.lib.helper.FileWriteHelper
import com.munch1182.android.lib.helper.RecordHelper
import com.munch1182.android.lib.helper.play
import com.munch1182.android.lib.helper.result.isAllGranted
import com.munch1182.android.lib.helper.result.permission
import com.munch1182.android.lib.helper.toAudio
import com.munch1182.p1.base.DialogHelper
import com.munch1182.p1.base.appIntent
import com.munch1182.p1.base.onDialog
import com.munch1182.p1.ui.ButtonState
import com.munch1182.p1.ui.IconStateOutlineButton
import com.munch1182.p1.ui.OutlineButtonState
import com.munch1182.p1.ui.SpacerV
import com.munch1182.p1.ui.StateButton
import com.munch1182.p1.ui.corner
import com.munch1182.p1.ui.theme.FontManySize
import com.munch1182.p1.ui.theme.PagePadding
import com.munch1182.p1.ui.theme.PagePaddingHalfLarge
import com.munch1182.p1.ui.weight.AudioFileInfo
import com.munch1182.p1.ui.weight.AudioInfoSheet
import com.munch1182.p1.ui.weight.DefaultFileExplorerConfig
import com.munch1182.p1.ui.weight.FileExplorer
import com.munch1182.p1.ui.weight.FileExplorerConfig
import com.munch1182.p1.ui.weight.FileExplorerVM
import com.munch1182.p1.ui.weight.FileFilter
import com.munch1182.p1.ui.weight.FileFilters
import com.munch1182.p1.ui.weight.FileItemData
import com.munch1182.p1.ui.weight.Loading
import com.munch1182.p1.ui.weight.getFileIcon
import com.munch1182.p1.ui.weight.getFileIconColor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

private val recordDir = FileHelper.newFile("audio")

@SuppressLint("MissingPermission")
@Composable
fun AudioView(record: RecordVM = viewModel(), play: AudioPlayVM = viewModel(), file: FileExplorerVM = viewModel()) {
    val uiState by record.uiState.collectAsState()

    LaunchedEffect(uiState.isRecording) { file.refreshFiles() }

    Column(Modifier.fillMaxWidth()) {
        StateButton(if (!uiState.isRecording) "开始录音" else "停止录音", uiState.isRecording) {
            withScanPermission { record.toggleRecord() }
        }
        if (!uiState.isRecording) {
            SpacerV()
            Column(
                Modifier
                    .wrapContentHeight()
                    .corner()
                    .background(Color.LightGray.copy(0.2f))

            ) {
                FileExplorer(recordDir, false, AudioFileConfig, vm = file, onFileClick = {
                    showFileView(it, record.newAudio, play) {
                        file.refreshFiles()
                    }
                })
            }
        }
    }
}

private fun showFileView(file: FileItemData, stream: AudioStreamHelper?, play: AudioPlayVM, refresh: () -> Unit) {
    DialogHelper.newBottom { fg ->
        val uiState by play.uiState.collectAsState()
        LaunchedEffect(file) { withIO { play.infoFile(file, stream) } }
        uiState.info?.let { audioInfo ->
            Column {
                AudioInfoSheet(audioInfo, onPlayPauseClick = { play.toggle(file, stream) }, onDeleteClick = {
                    file.file.delete()
                    refresh()
                    fg.dismiss()
                })
                ClickItem(
                    OutlineButtonState(Icons.Default.SwapVert, "转换WAV"), {},
                    OutlineButtonState(Icons.Default.ForkLeft, "单声道"), {})
            }
        } ?: Loading()
    }.show()
}

@Composable
private fun ClickItem(
    left: OutlineButtonState, leftClick: () -> Unit, right: OutlineButtonState? = null, rightClick: () -> Unit = { }
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = PagePadding), horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconStateOutlineButton(left, onClick = leftClick, Modifier.weight(1f))
        right?.let {
            Spacer(modifier = Modifier.width(12.dp))
            IconStateOutlineButton(it, onClick = rightClick, Modifier.weight(1f))
        }
    }
    SpacerV()
}

@Stable
class RecordVM : ViewModel() {
    private val _uiState = MutableStateFlow(RecordUIState())
    private val recordHelper = RecordHelper.recognition()
    private val writerHelper = FileWriteHelper()
    private val recordJob = ReRunJob()

    val uiState = _uiState.asStateFlow()
    private val format by lazy { SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()) }

    val newAudio get() = runCatching { recordHelper.toAudio() }.getOrNull()

    override fun onCleared() {
        super.onCleared()
        stopRecord()
        recordHelper.release()
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun toggleRecord() {
        if (_uiState.value.isRecording) stopRecord() else startRecord()
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun startRecord() {
        viewModelScope.launchIO(recordJob.newContext) {
            _uiState.update { it.copy(isRecording = true) }
            val newFile = File(recordDir, "${format.format(System.currentTimeMillis())}.pcm")
            writerHelper.prepare(newFile, true)

            recordHelper.record().collect { writerHelper.write(it) }
        }
    }

    private fun stopRecord() {
        recordJob.cancel()
        writerHelper.complete()
        _uiState.update { it.copy(isRecording = false) }
    }
}

@Stable
class AudioPlayVM : ViewModel() {
    private val _uiState = MutableStateFlow(AudioPlayUIState())
    private var audio: AudioStreamHelper? = null
    private val playJob = ReRunJob()
    val uiState = _uiState.asStateFlow()

    override fun onCleared() {
        super.onCleared()
        stop()
        audio?.release()
    }

    fun play(audio: AudioStreamHelper?, file: File) {
        audio ?: return
        this.audio = audio
        viewModelScope.launchIO(playJob.newContext) {
            audio.prepare()
            _uiState.update { it.copy(state = ButtonState.Running, info = it.info?.copy(isPlaying = true)) }
            audio.play(file)
            _uiState.update { it.copy(state = ButtonState.Ready, info = it.info?.copy(isPlaying = false)) }
        }
    }

    fun stop() {
        audio?.stop()
        playJob.cancel()
        _uiState.update { it.copy(state = ButtonState.Ready, info = it.info?.copy(isPlaying = false)) }
    }

    fun toggle(file: FileItemData, stream: AudioStreamHelper?) {
        if (_uiState.value.state.isRunning) stop() else play(stream, file.file)
    }

    fun infoFile(file: FileItemData, stream: AudioStreamHelper?) {
        val fileName = file.file.name
        val fileSize = file.size.formatFileSize()
        val duration = when (file.extension) {
            "pcm" -> stream?.let {
                file.file.getPcmDuration(it.sampleRate, it.frameSizeInBytes)
            } ?: 0L

            else -> file.file.getAudioDuration().takeIf { it != -1L } ?: 0L
        }.toDurationStrSmart()
        val format = file.extension
        val filePath = file.file.absolutePath
        _uiState.update {
            it.copy(info = AudioFileInfo(fileName, fileSize, duration, format, filePath))
        }
    }
}

@Stable
data class RecordUIState(val isRecording: Boolean = false)

@Stable
data class AudioPlayUIState(val state: ButtonState = ButtonState.Ready, val info: AudioFileInfo? = null)


@Stable
private object AudioFileConfig : FileExplorerConfig {
    override val showFileSize: Boolean = true
    override val showFileExtension: Boolean = true
    override val allowFileSelection: Boolean = true
    override val showRootPath: Boolean get() = false
    override val filter: FileFilter get() = FileFilters.AUDIO


    @Composable
    override fun FileItemContent(file: FileItemData, isClickable: Boolean, onClick: () -> Unit, onLongClick: (() -> Unit)?) {
        val backgroundColor = if (file.isDirectory) Color.LightGray.copy(alpha = 0.1f) else Color.Transparent

        Row(
            verticalAlignment = Alignment.CenterVertically, modifier = Modifier
                .fillMaxWidth()
                .corner()
                .clickable(
                    enabled = isClickable, onClick = onClick, indication = LocalIndication.current, interactionSource = remember { MutableInteractionSource() })
                .apply {
                    if (onLongClick != null) {
                        combinedClickable(onClick = onClick, onLongClick = onLongClick)
                    }
                }
                .background(backgroundColor)
                .padding(horizontal = PagePadding, vertical = PagePaddingHalfLarge)) {
            Icon(
                painter = rememberVectorPainter(image = getFileIcon(file)), contentDescription = if (file.isDirectory) "文件夹" else "文件", modifier = Modifier.size(32.dp), tint = getFileIconColor(file)
            )

            Spacer(modifier = Modifier.width(PagePadding))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.displayName, fontSize = 16.sp, fontWeight = if (file.isDirectory) FontWeight.Bold else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis
                )

                if (DefaultFileExplorerConfig.showFileSize && !file.isDirectory && !file.isBackItem) {
                    Text(
                        text = file.size.formatFileSize(), fontSize = FontManySize, color = Color.Gray, modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

private inline fun withScanPermission(crossinline any: () -> Unit) {
    permission(Manifest.permission.RECORD_AUDIO).onDialog("录音", "音频录制").appIntent().request {
        if (it.isAllGranted()) any()
    }
}