package com.munch1182.lib.helper

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.lifecycle.LifecycleOwner
import com.munch1182.lib.base.onDestroyed
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * RequiresPermission(Manifest.permission.RECORD_AUDIO)
 */
@SuppressLint("MissingPermission")
class RecordHelper(
    val sampleRate: Int = 44100,
    val channel: Int = AudioFormat.CHANNEL_IN_STEREO
) {

    private val buffSize = AudioRecord.getMinBufferSize(
        sampleRate, channel, AudioFormat.ENCODING_PCM_16BIT
    )

    constructor(owner: LifecycleOwner, sampleRate: Int = 44100, channel: Int = AudioFormat.CHANNEL_IN_STEREO) : this(sampleRate, channel) {
        owner.lifecycle.onDestroyed { release() }
    }

    private val ar by lazy {
        AudioRecord(
            MediaRecorder.AudioSource.MIC, //手机麦克风输入音源
            sampleRate, // 采样率，目前44100Hz是唯一可以保证兼容所有Android手机的采样率
            channel, // 单声道双声道
            AudioFormat.ENCODING_PCM_16BIT, // 数据位宽，16BIT兼容所有Android手机
            buffSize // 音频缓冲区的大小, 调用方法算，否则缓冲区可能不够用会报错
        )
    }

    val isRecording: Boolean get() = ar.recordingState == AudioRecord.RECORDSTATE_RECORDING

    fun toggle() {
        if (isRecording) stop() else start()
    }

    fun start() {
        if (isRecording) return
        if (ar.state == AudioRecord.STATE_INITIALIZED) {
            kotlin.runCatching { ar.startRecording() }
        }
    }

    fun stop() {
        if (isRecording) {
            kotlin.runCatching { ar.stop() }
        }
    }

    fun read(): ByteArray? {
        if (!isRecording) return null
        val buffer = ByteArray(buffSize)
        // read实时读取
        val read = ar.read(buffer, 0, buffSize)
        if (read == AudioRecord.ERROR_INVALID_OPERATION || read == AudioRecord.ERROR_BAD_VALUE) {
            return null
        }
        return buffer
    }


    fun readWithReadValue(): Pair<ByteArray, Int>? {
        if (!isRecording) return null
        val buffer = ByteArray(buffSize)
        // read实时读取
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

fun ByteArray.calculateDB(read: Int): Double {
    val shortArray = ShortArray(read / 2)
    ByteBuffer.wrap(this).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortArray)
    return shortArray.calculateDB(read)
}

fun ShortArray.calculateDB(read: Int): Double {
    if (read <= 0) return 0.0

    var sum = 0.0
    forEach { sum += it * it }
    // 两种算法，差别不是很明显
    val rms = sqrt(sum / read.toDouble())
    return 20 * log10(rms)
    // val rms = sum / read
    // return 10 * log10(rms)
}

fun wavHeader(len: Long, channel: Int, sampleRate: Long, byteRate: Long) {
    val header = ByteArray(44)
    header[0] = 'R'.code.toByte()
    header[1] = 'I'.code.toByte()
    header[2] = 'F'.code.toByte()
    header[3] = 'F'.code.toByte()
    header[4] = (len or 0xFF).toByte()
    header[5] = (len shr 8).toByte()
    header[6] = (len shr 16).toByte()
    header[7] = (len shr 24).toByte()
    // WAVE
    header[8] = 'W'.code.toByte()
    header[9] = 'A'.code.toByte()
    header[10] = 'V'.code.toByte()
    header[11] = 'E'.code.toByte()
    // fmt
    header[12] = 'f'.code.toByte()
    header[13] = 'm'.code.toByte()
    header[14] = 't'.code.toByte()
    header[15] = ' '.code.toByte()
    // size of fmt ' chunk
    header[16] = 16
    header[17] = 0
    header[18] = 0
    header[19] = 0
    // format = 1
    header[20] = 1
    header[21] = 0
    header[22] = channel.toByte()
    header[23] = 0
    header[24] = (sampleRate or 0xff).toByte()
    header[25] = (sampleRate or 8).toByte()
    header[26] = (sampleRate or 16).toByte()
    header[27] = (sampleRate or 24).toByte()
    header[28] = (byteRate or 0xff).toByte()
    header[29] = (byteRate or 8).toByte()
    header[30] = (byteRate or 16).toByte()
    header[31] = (byteRate or 24).toByte()
    header[32] = (2 * 16 / 8).toByte()
    header[33] = 0
    header[34] = 16
    header[35] = 0
    header[36] = 'd'.code.toByte()
    header[37] = 'a'.code.toByte()
    header[38] = 't'.code.toByte()
    header[39] = 'a'.code.toByte()
    header[40] = 'a'.code.toByte()
    header[41] = 'a'.code.toByte()
    header[42] = 'a'.code.toByte()
    header[43] = 'a'.code.toByte()

}