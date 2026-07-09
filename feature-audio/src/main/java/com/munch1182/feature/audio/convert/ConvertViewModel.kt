package com.munch1182.feature.audio.convert

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.munch1182.lib.android.copy2File
import com.munch1182.lib.android.newFile
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
    private val dir = newFile("audio")

    init {
        viewModelScope.launchIO {
            val defaultConfigFile = File(dir, "config-audio-convert.json")
            if (defaultConfigFile.exists()) loadConfig(defaultConfigFile)
        }
    }

    /**
     * 从文件中加载音频转换配置
     */
    fun loadConfig(file: File) {
        viewModelScope.launchIO {
            try {
                val toolConfig = parse(file.readText()) // 返回 ToolConfig
                _uiState.value = _uiState.value.copy(toolConfig = toolConfig)
                // 默认选择第一个格式
                toolConfig.formats.firstOrNull()?.let { selectFormat(it) }
            } catch (e: IOException) {
                _uiState.value = _uiState.value.copy(error = Error.File(e.message ?: ""))
            } catch (e: SerializationException) {
                _uiState.value = _uiState.value.copy(error = Error.Serialization(e.message ?: ""))
            } catch (e: IllegalStateException) {
                _uiState.value = _uiState.value.copy(error = Error.CommandInvalid(e.message ?: ""))
            }
        }
    }

    /**
     * 选择输出格式
     */
    fun selectFormat(format: ConvertFormat) {
        val tool = _uiState.value.toolConfig ?: return
        // 获取每个参数默认值（第一个选项）
        val paramValues = try {
            format.params.associateWith { key ->
                tool.params[key]?.firstOrNull()
                    ?: throw IllegalStateException("Missing default value for param: $key")
            }
        } catch (e: IllegalStateException) {
            _uiState.value = _uiState.value.copy(error = Error.CommandInvalid(e.message ?: ""))
            return
        }

        // 替换格式模板中的 {{key}}
        val formatCmd = try {
            format.cmd.replaceTemplate(paramValues)
        } catch (e: IllegalArgumentException) {
            _uiState.value = _uiState.value.copy(error = Error.CommandInvalid(e.message ?: ""))
            return
        }

        // 构建完整命令
        val fullCmd = tool.buildCommand(format, formatCmd, _uiState.value.currFile)

        _uiState.value = _uiState.value.copy(
            currentFormatState = FormatState(
                format = format,
                params = paramValues,
                formatCmd = formatCmd,
                fullCmd = fullCmd
            ),
            error = null
        )
    }

    /**
     * 选择输入文件
     */
    fun selectFile(uri: Uri) {
        viewModelScope.launchIO {
            val file = uri.copy2File(dir)
            if (file != null) {
                _uiState.value = _uiState.value.copy(currFile = file)
                // 如果有选中的格式，刷新完整命令（因为文件路径变化）
                val state = _uiState.value.currentFormatState
                if (state != null) {
                    val tool = _uiState.value.toolConfig ?: return@launchIO
                    val newFull = tool.buildCommand(state.format, state.formatCmd, file)
                    _uiState.value = _uiState.value.copy(
                        currentFormatState = state.copy(fullCmd = newFull)
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(error = Error.File("选择的文件读取失败"))
            }
        }
    }

    /**
     * 更新某个参数的值
     */
    fun updateParam(key: String, value: Option) {
        val currentState = _uiState.value.currentFormatState ?: return
        val newParams = currentState.params.toMutableMap().apply { put(key, value) }

        // 重新生成格式命令
        val newFormatCmd = try {
            currentState.format.cmd.replaceTemplate(newParams)
        } catch (e: IllegalArgumentException) {
            _uiState.value = _uiState.value.copy(error = Error.CommandInvalid(e.message ?: ""))
            return
        }

        // 重新构建完整命令
        val tool = _uiState.value.toolConfig ?: return
        val newFull = tool.buildCommand(currentState.format, newFormatCmd, _uiState.value.currFile)

        _uiState.value = _uiState.value.copy(
            currentFormatState = currentState.copy(
                params = newParams,
                formatCmd = newFormatCmd,
                fullCmd = newFull
            ),
            error = null
        )
    }
}

/**
 * UI 状态
 */
internal data class ConvertUiState(
    val currFile: File? = null,
    val toolConfig: ToolConfig? = null,
    val currentFormatState: FormatState? = null,
    val error: Error? = null,
    val isLoading: Boolean = false
)

/**
 * 当前格式的完整状态（包含参数和命令）
 */
internal data class FormatState(
    val format: ConvertFormat,
    val params: Map<String, Option>,   // 参数名 -> 当前选中的 Option
    val formatCmd: String,             // 已替换 {{key}} 的编码命令
    val fullCmd: String                // 包含 from/to 的完整命令
)

/**
 * 错误类型
 */
internal sealed class Error(val str: String) {
    data class File(val err: String) : Error("文件异常: $err")
    data class Serialization(val err: String) : Error("序列化错误: $err")
    data class CommandInvalid(val err: String) : Error("命令生成错误: $err")
}