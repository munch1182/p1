package com.munch1182.lib.other

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlin.math.log10
import kotlin.math.sqrt


/**
 * RequiresPermission(Manifest.permission.RECORD_AUDIO)
 */
@SuppressLint("MissingPermission")
class RecordManager(
    val sampleRate: Int = 44100,
    val channel: Int = AudioFormat.CHANNEL_IN_STEREO
) {

    private var _isRecording = false
    private val buffSize = AudioRecord.getMinBufferSize(
        sampleRate, channel, AudioFormat.ENCODING_PCM_16BIT
    )


    private val ar by lazy {
        AudioRecord(
            MediaRecorder.AudioSource.MIC, //手机麦克风输入音源
            sampleRate, // 采样率，目前44100Hz是唯一可以保证兼容所有Android手机的采样率
            channel, // 单声道双声道
            AudioFormat.ENCODING_PCM_16BIT, // 数据位宽，16BIT兼容所有Android手机
            buffSize // 音频缓冲区的大小, 调用方法算，否则缓冲区可能不够用会报错
        )
    }

    val isRecording: Boolean
        get() = _isRecording

    fun toggle() {
        if (_isRecording) stop() else start()
    }

    fun start() {
        if (_isRecording) return
        if (ar.state == AudioRecord.STATE_INITIALIZED) {
            kotlin.runCatching { ar.startRecording() }
            _isRecording = true
        }
    }

    fun stop() {
        if (_isRecording) {
            kotlin.runCatching { ar.stop() }
            _isRecording = false
        }
    }

    fun read(): ShortArray? {
        if (!_isRecording) return null
        val buffer = ShortArray(buffSize)
        val read = ar.read(buffer, 0, buffSize)
        if (read == AudioRecord.ERROR_INVALID_OPERATION || read == AudioRecord.ERROR_BAD_VALUE) {
            return null
        }
        return buffer
    }

    fun readWithReadValue(): Pair<ShortArray, Int>? {
        if (!_isRecording) return null
        val buffer = ShortArray(buffSize)
        val read = ar.read(buffer, 0, buffSize)
        if (read == AudioRecord.ERROR_INVALID_OPERATION || read == AudioRecord.ERROR_BAD_VALUE) {
            return null
        }
        return buffer to read
    }


    fun release() {
        stop()
        ar.release()
    }
}

fun ShortArray.calculateDB(read: Int): Double {
    if (read <= 0) return 0.0

    var sum = 0.0
    (0 until read).forEach {
        sum += this[it] * this[it]
    }
    // 两种算法，差别不是很明显
    val rms = sqrt(sum / read)
    return 20 * log10(rms)
    // val rms = sum / read
    // return 10 * log10(rms)
}