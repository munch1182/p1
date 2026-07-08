package com.munch1182.feature.audio.convert

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.munch1182.lib.android.copy2File
import com.munch1182.lib.android.newCache
import com.munch1182.lib.common.launchIO
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.SerializationException
import java.io.File
import java.io.IOException

@HiltViewModel
class ConvertViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(ConvertUiState())
    internal val uiState = _uiState.asStateFlow()
    private val dir = newCache("audio")

    init {
        viewModelScope.launchIO {
            val defaultConfigFile = File(dir, "default-audio-convert.json")
            if (defaultConfigFile.exists()) loadConfig(defaultConfigFile)
        }
    }

    /**
     * 从文件中加载音频相关配置
     */
    fun loadConfig(file: File) {
        viewModelScope.launchIO {
            try {
                val config = parse(file.readText())
                _uiState.value = _uiState.value.copy(config = config)
                config.formats.firstOrNull()?.let { selectFormat(it) }
            } catch (e: IOException) {
                _uiState.value = _uiState.value.copy(error = Error.File(e.message ?: ""))
            } catch (e: SerializationException) {
                _uiState.value = _uiState.value.copy(error = Error.Serialization(e.message ?: ""))
            }
        }
    }

    fun selectFormat(format: ConvertFormat) {
        val cfg = _uiState.value.config ?: return
        // 取每个参数的默认值（第一个选项）
        val paramValues = format.params.associateWith { key ->
            cfg.params[key]?.firstOrNull() ?: "" // 默认空字符串，也可抛异常
        }

        // 生成命令
        val cmd = try {
            format.cmd.replaceTemplate(paramValues)
        } catch (e: IllegalStateException) {
            _uiState.value = _uiState.value.copy(error = Error.CommandInvalid(e.message ?: ""))
            return
        }
        _uiState.value = _uiState.value.copy(
            currentFormatState = FormatState(format = format, params = paramValues, cmd = cmd),
            error = null
        )
    }

    fun selectFile(uri: Uri) {
        viewModelScope.launchIO {
            val file = uri.copy2File(dir)
            if (file != null) {
                _uiState.value = _uiState.value.copy(currFile = file)
            } else {
                _uiState.value = _uiState.value.copy(error = Error.File("选择的文件读取失败"))
            }
        }
    }

    fun updateParam(key: String, value: String) {
        val currentState = _uiState.value.currentFormatState ?: return
        val newParams = currentState.params.toMutableMap().apply { put(key, value) }

        // 重新生成命令
        val newCmd = try {
            currentState.format.cmd.replaceTemplate(newParams)
        } catch (e: IllegalStateException) {
            _uiState.value = _uiState.value.copy(error = Error.CommandInvalid(e.message ?: ""))
            return
        }
        _uiState.value = _uiState.value.copy(
            currentFormatState = currentState.copy(params = newParams, cmd = newCmd),
            error = null
        )
    }
}

internal data class ConvertUiState(
    // 文件相关
    val currFile: File? = null,
    // 配置相关（整体）
    val config: ConvertConfig? = null,
    // 当前选中的格式及其参数、命令
    val currentFormatState: FormatState? = null,
    // 错误信息
    val error: Error? = null,
    // 是否正在加载
    val isLoading: Boolean = false
)

internal data class FormatState(
    val format: ConvertFormat, //
    val params: Map<String, String>, // 参数名 -> 当前选中值（非空）
    val cmd: String                  // 最终生成的命令
)

internal sealed class Error(val str: String) {
    data class File(val err: String) : Error("文件异常: $err")
    data class Serialization(val err: String) : Error("序列化错误: $err")
    data class CommandInvalid(val err: String) : Error("名字生成错误: $err")
}