package com.munch1182.lib.android.audio


import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.os.Build
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * 兼容方式获取 AudioFormat 的声道数（Channel Count）。
 * - API 29 以上 直接使用系统的 [AudioFormat.getChannelCount]。
 * - API 26 至 28 优先通过 channelMask 计算，其次通过 channelIndexMask 计算。
 */
val AudioFormat.channelCountCompat: Int
    get() {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            channelCount
        } else {
            var count = channelMask.countOneBits()
            // 补充对 API 24+ channelIndexMask 的支持，确保与“完全兼容”的契约一致
            if (count == 0) count = channelIndexMask.countOneBits()
            if (count == 0) 1 else count
        }
    }


/**
 * 兼容方式获取 AudioFormat 的帧大小（字节数）。
 * 帧大小 = 每个采样的字节数 × 声道数。
 * - API 29+ 直接使用 [android.media.AudioFormat.getFrameSizeInBytes]
 * - 低版本通过 [frameSizeInBytes] 辅助函数根据编码和声道数计算。
 */
val AudioFormat.frameSizeInBytesCompat
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        frameSizeInBytes
    } else {
        // see android.media.AudioFormat#getBytesPerSample
        frameSizeInBytes(encoding, channelCountCompat)
    }

/**
 * 根据音频编码和声道数计算每帧字节数。
 * 参考 [android.media.AudioFormat#getBytesPerSample] 的内部实现
 *
 * @param encoding 音频编码格式，如 [AudioFormat.ENCODING_PCM_16BIT]
 * @param channelCount see [AudioFormat.channelCountCompat]
 * @return 每帧字节数
 */
fun frameSizeInBytes(encoding: Int, channelCount: Int) = getBytesPerSample(encoding) * channelCount

/**
 * 每个采样的字节数
 *
 * @see android.media.AudioFormat#getBytesPerSample
 */
fun getBytesPerSample(encoding: Int) = when (encoding) {
    AudioFormat.ENCODING_PCM_8BIT -> 1
    AudioFormat.ENCODING_PCM_16BIT, AudioFormat.ENCODING_IEC61937, AudioFormat.ENCODING_DEFAULT -> 2
    AudioFormat.ENCODING_PCM_24BIT_PACKED -> 3
    AudioFormat.ENCODING_PCM_FLOAT, AudioFormat.ENCODING_PCM_32BIT -> 4
    else -> 2
}

/**
 * 计算 16-bit little-endian PCM 音频帧的 dBFS 值
 * 基于字节数组直接解析，避免创建 ByteBuffer，性能更优。
 * 返回值范围约为 [-96, 0] dBFS，静音返回 -96
 */
fun ByteArray.calculateDBFS(read: Int = size): Double {
    val validShortCount = read / 2
    if (validShortCount == 0) return -96.0

    var sum = 0L  // 使用 Long 累加，避免浮点溢出（虽然 16bit 不会，但好习惯）
    var i = 0
    while (i + 1 < read) {
        // 小端序：低字节在前，高字节在后，合成有符号 16-bit 整数
        val low = this[i].toInt() and 0xFF
        val high = this[i + 1].toInt() and 0xFF
        val sample = ((high shl 8) or low).toShort().toInt()
        sum += sample * sample
        i += 2
    }

    val rms = sqrt(sum.toDouble() / validShortCount)
    if (rms < 1.0) return -96.0
    return 20 * log10(rms / 32768.0)
}

/**
 * 默认的音频属性（语音通信、语音内容）
 * 当清楚音频焦点时, 需要传入一致的参数
 */
fun newAudioAttributes(
    usage: Int = AudioAttributes.USAGE_VOICE_COMMUNICATION,
    contentType: Int = AudioAttributes.CONTENT_TYPE_SPEECH
): AudioAttributes = AudioAttributes.Builder()
    .setUsage(usage)
    .setContentType(contentType)
    .build()

/**
 * 请求音频焦点。
 * 适用于 API 26+。
 */
fun AudioManager.requestAudioFocus(
    focusGain: Int = AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE,
    onFocusChangeListener: AudioManager.OnAudioFocusChangeListener? = null,
    audioAttributes: AudioAttributes = newAudioAttributes()
): Boolean {
    val builder = AudioFocusRequest.Builder(focusGain).setAudioAttributes(audioAttributes)
    if (onFocusChangeListener != null) builder.setOnAudioFocusChangeListener(onFocusChangeListener)
    return requestAudioFocus(builder.build()) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
}

/**
 * 释放音频焦点。
 * 传入的三个参数需要与请求焦点时的对象或者参数一致
 */
fun AudioManager.abandonAudioFocus(
    focusGain: Int = AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE,
    onFocusChangeListener: AudioManager.OnAudioFocusChangeListener? = null,
    audioAttributes: AudioAttributes = newAudioAttributes()
): Boolean {
    val builder = AudioFocusRequest.Builder(focusGain).setAudioAttributes(audioAttributes)
    if (onFocusChangeListener != null) builder.setOnAudioFocusChangeListener(onFocusChangeListener)
    return abandonAudioFocusRequest(builder.build()) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
}