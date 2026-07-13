package com.munch1182.feature.audio.record

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
internal class RecordViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(RecordUiState())
    val uiState = _uiState.asStateFlow()

    fun toggleRecord() {
        if (_uiState.value.isRecording) {
            stopRecord()
        } else {
            startRecord()
        }
    }

    private fun startRecord() {}

    private fun stopRecord() {}

    fun updateConfig(copy: RecordConfig) {
        _uiState.update { it.copy(cfg = copy) }
    }
}

internal data class RecordUiState(
    val isRecording: Boolean = false,
    val currDBFS: Double = 0.0,
    val shouldUpload: Boolean = false,
    val cfg: RecordConfig = RecordConfig()
)

internal data class RecordConfig(
    val sampleRate: Int = 44100,
    val channelCount: Int = 1,
    val audioFormat: Int = 2, // PCM 16bit
)