package com.munch1182.lib.android.audio


import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max

/**
 * 专用于语音识别（ASR）的 16kHz 单声道 AudioRecord。
 */
val newRecognitionAudioRecord
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    get() = newAudioRecord(sampleRateInHz = 16000)

/**
 * 兼容性更好的 44.1kHz 通用型 AudioRecord。
 */
val newCompatAudioRecord
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    get() = newAudioRecord(sampleRateInHz = 44100)

/**
 * 工厂方法：构建一个安全的 AudioRecord 实例。
 */
@RequiresPermission(Manifest.permission.RECORD_AUDIO)
fun newAudioRecord(
    sampleRateInHz: Int,
    channelConfig: Int = AudioFormat.CHANNEL_IN_MONO,
    audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT,
    buffSizeInBytes: Int? = null
): AudioRecord {
    val format = AudioFormat.Builder()
        .setSampleRate(sampleRateInHz)
        .setChannelMask(channelConfig)
        .setEncoding(audioFormat)
        .build()
    val minBufferSize = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat)
    val finalBuffSize = buffSizeInBytes?.let { max(it, minBufferSize) } ?: (minBufferSize * 2)
    return AudioRecord.Builder()
        .setAudioSource(MediaRecorder.AudioSource.MIC)
        .setAudioFormat(format)
        .setBufferSizeInBytes(finalBuffSize)
        .build()
}

/**
 * 根据采样率、位深和目标帧时长，计算单次读取的理想字节数。
 * @param sampleRateHz 采样率（如 16000）
 * @param bytesPerSample 每样本字节数（16bit=2, 8bit=1）
 * @param frameDurationMs 目标帧时长（毫秒），默认 64ms
 * @param minBytes 最小返回字节数，防止极端短帧导致 CPU 空转，默认 160 字节（约 5ms@16kHz）
 */
fun computeBatchSize(
    sampleRateHz: Int = 16000,
    bytesPerSample: Int = 2,
    frameDurationMs: Int = 64,
    minBytes: Int = 160
): Int {
    val rawBytes = (sampleRateHz * frameDurationMs / 1000.0) * bytesPerSample
    return max(minBytes, rawBytes.toInt())
}

/**
 * 执行录音并通过 Flow 实时返回音频原始数据段。
 *
 * ## 核心读取策略
 * - 使用非阻塞模式（`READ_NON_BLOCKING`），保证协程响应性。
 * - **连续读取**：当 `read()` 返回正数时，立即发送数据并进入下一轮循环，不挂起，以最低延迟清空缓冲区。
 * - **延迟等待**：当 `read()` 返回 `0` 时，说明硬件缓冲区暂无新数据，主动 `delay(5)` 让出 CPU，避免空转。
 *
 * ## 各场景行为说明
 * 1. **启动时**：检查 `AudioRecord` 状态，若未录音则调用 `startRecording()`。首次循环立即尝试读取，有数据则发送，无则等待。
 * 2. **系统切换 / 负载升高**：`read()` 频繁返回 `0`，触发 5ms 延迟等待，降低 CPU 占用，同时保持协程可取消。
 * 3. **正常稳定运行**：`read()` 返回实际可读字节数（通常小于 `batchSizeBytes`），循环无延迟执行，以最快速度上送数据。
 *
 * ## 为何要尽可能快读取
 * 最小化音频从麦克风到应用层的延迟，避免缓冲区积压。语音识别等实时场景依赖低延迟。
 * 本函数仅提供原始 PCM 流，外部如需固定时间/大小窗口，请通过 Flow 操作符（如 `buffer`、`chunked`）二次处理。
 *
 * ## 缓冲区与溢出处理（重要）
 * - **缓冲区大小**：指构造 `AudioRecord` 时传入的 `bufferSizeInBytes`，决定了内部环形缓冲区可缓存的最大数据量。
 * - **溢出发生（物理层）**：当读取速度慢于生产速度时，环形缓冲区写满，新数据会物理覆盖未被读取的旧数据（Overrun）。
 * - **API 反馈（应用层）**：覆盖发生后，读写指针逻辑关系被破坏，底层判定数据流不可靠。
 *   此时后续 `read()` **不会返回覆盖后的新数据**，而是返回 `ERROR_INVALID_OPERATION`（-3）等负值，强制应用感知异常。
 *
 * @param batchSizeBytes 单次读取的最大字节数。实际读取量通常小于该值。
 *    建议通过 [computeBatchSize] 根据采样率和目标帧时长自动计算，不建议超过 [AudioRecord] 的缓冲区大小。
 * @see computeBatchSize
 */
fun AudioRecord.record(batchSizeBytes: Int) = channelFlow {
    if (state != AudioRecord.STATE_INITIALIZED) {
        close(IllegalStateException("AudioRecord not initialized"))
        return@channelFlow
    }

    // 单次从硬件通道拉取的数据块大小（
    val buff = ByteArray(batchSizeBytes)

    val recordJob = launch {
        try {
            if (recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                startRecording()
            }

            while (recordingState == AudioRecord.RECORDSTATE_RECORDING && coroutineContext.isActive) {

                // 发起非阻塞读取
                val bytesRead = read(buff, batchSizeBytes, AudioRecord.READ_NON_BLOCKING)

                when {
                    // 情况 A：硬件缓冲区有积压数据。此时不执行任何 delay 挂起，直接进入下轮 while，以最高速度抽干缓冲区
                    bytesRead > 0 -> send(buff.copyOf(bytesRead))
                    // 情况 B：底层水管暂时空了（读取速度超越了硬件产生速度）。
                    // 挂起 5 毫秒让出 CPU 线程所有权，防止 while 变成空转死循环导致 CPU 飙升。
                    // 同时，这 5 毫秒挂起让协程有机会检测外部的 isActive 取消信号。
                    bytesRead == 0 -> delay(5)
                    // 情况 C：硬件或系统底层异常
                    else -> { // 所有负数统一处理
                        close(IllegalStateException("Read error: $bytesRead"))
                        break
                    }
                }
            }
        } catch (e: Exception) {
            close(e)
        }
    }

    // 如果一直循环, 会阻止awaitClose注册, 导致无法stop
    // 当 Flow 收集者正常关闭、取消、或者页面销毁触发作用域取消时，确保录音停止
    awaitClose {
        recordJob.cancel()
        runCatching {
            if (recordingState == AudioRecord.RECORDSTATE_RECORDING) stop()
        }
    }
}