package com.munch1182.lib.helper.sound

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
 * 录音
 *
 * RecordHelper必须在有录音权限之后创建，否则会开始录音失败
 *
 * @see start 开始录音
 * @see stop 停止录音
 * @see release 释放录音
 * @see isRecording 是否正在录音
 *
 * @see record 实时录音，循环读取即是录音数据
 * @see newBuffer 获取一个合适的录音缓冲区
 *
 * RequiresPermission(Manifest.permission.RECORD_AUDIO)
 */
@SuppressLint("MissingPermission")
class RecordHelper(val sampleRate: Int = 44100, val channel: Int = AudioFormat.CHANNEL_IN_MONO, val format: Int = AudioFormat.ENCODING_PCM_16BIT) {

    /**
     * @see newBuffer
     */
    val buffSize = AudioRecord.getMinBufferSize(sampleRate, channel, format) * 2

    constructor(
        owner: LifecycleOwner, sampleRate: Int = 44100, channel: Int = AudioFormat.CHANNEL_IN_MONO, format: Int = AudioFormat.ENCODING_PCM_16BIT
    ) : this(sampleRate, channel, format) {
        owner.lifecycle.onDestroyed { release() }
    }

    private val ar by lazy {
        AudioRecord(
            MediaRecorder.AudioSource.MIC, // 手机麦克风输入音源，VOICE_COMMUNICATION会降噪但是声音会变小
            sampleRate, // 采样率，目前44100Hz是唯一可以保证兼容所有Android手机的采样率
            channel, // 单声道双声道
            format, // 数据位宽，16BIT兼容所有Android手机
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

    val newBuffer: ByteArray get() = ByteArray(buffSize)

    fun record(buffer: ByteArray): Int? {
        if (!isRecording) return null
        // read实时读取
        val read = ar.read(buffer, 0, buffSize)
        if (read == AudioRecord.ERROR_INVALID_OPERATION || read == AudioRecord.ERROR_BAD_VALUE) {
            return null
        }
        return read
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
    if (sum == 0.0) return 0.0
    // 两种算法，差别不是很明显
    val rms = sqrt(sum / read.toDouble())
    return 20 * log10(rms)
    // val rms = sum / read
    // return 10 * log10(rms)
}

fun RecordHelper.wavHeader(dataSize: Long) = wavHeader(
    sampleRate, if (channel == AudioFormat.CHANNEL_IN_MONO) 1 else 2, if (format == AudioFormat.ENCODING_PCM_16BIT) 16 else 8, dataSize
)

/**
 * 将pcm格式转为wav格式
 */
fun wavHeader(sampleRate: Int, channel: Int, bitsPerSample: Int, dataSize: Long): ByteArray {
    val byteRate = sampleRate * channel * bitsPerSample / 8
    val byteCount = channel * bitsPerSample / 8
    val totalDataSize = 36L + dataSize

    val header = ByteArray(44)
    header[0] = 'R'.code.toByte()
    header[1] = 'I'.code.toByte()
    header[2] = 'F'.code.toByte()
    header[3] = 'F'.code.toByte()
    header[4] = (totalDataSize and 0xff).toByte()
    header[5] = ((totalDataSize shr 8) and 0xff).toByte()
    header[6] = ((totalDataSize shr 16) and 0xff).toByte()
    header[7] = ((totalDataSize shr 24) and 0xff).toByte()
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
    // SubChunk
    header[16] = 16
    header[17] = 0
    header[18] = 0
    header[19] = 0
    // format = 1
    header[20] = 1
    header[21] = 0
    header[22] = (channel and 0xFF).toByte()
    header[23] = (channel shr 8 and 0xFF).toByte()

    header[24] = (sampleRate and 0xff).toByte()
    header[25] = ((sampleRate shr 8) and 0xff).toByte()
    header[26] = ((sampleRate shr 16) and 0xff).toByte()
    header[27] = ((sampleRate shr 24) and 0xff).toByte()
    header[28] = (byteRate or 0xff).toByte()
    header[29] = ((byteRate shr 8) and 0xff).toByte()
    header[30] = ((byteRate shr 16) and 0xff).toByte()
    header[31] = ((byteRate shr 24) and 0xff).toByte()
    header[32] = (byteCount and 0xFF).toByte()
    header[33] = (byteCount shr 8 and 0xFF).toByte()
    header[34] = (bitsPerSample and 0xFF).toByte()
    header[35] = (bitsPerSample shr 8 and 0xFF).toByte()
    // data sub-chunk
    header[36] = 'd'.code.toByte()
    header[37] = 'a'.code.toByte()
    header[38] = 't'.code.toByte()
    header[39] = 'a'.code.toByte()
    header[40] = (dataSize and 0xff).toByte()
    header[41] = ((dataSize shr 8) and 0xff).toByte()
    header[42] = ((dataSize shr 16) and 0xff).toByte()
    header[43] = ((dataSize shr 24) and 0xff).toByte()
    return header
}