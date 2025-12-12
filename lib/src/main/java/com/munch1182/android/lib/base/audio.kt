package com.munch1182.android.lib.base

import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.File

/**
 * 通过pcm文件大小和其参数获取其播放长度(s); 如果获取失败，返回-1
 */
fun File.getPcmDuration(sampleRate: Int, frameSizeInBytes: Int): Long {
    try {
        val totalSamples = length() / frameSizeInBytes
        return (totalSamples * 1000L) / sampleRate
    } catch (e: Exception) {
        e.printStackTrace()
        return -1L
    }
}

/**
 * 获取编码音频文件中的音频时长数据(s); 如果获取失败，返回-1
 */
fun File.getAudioDuration(): Long {
    val extractor = MediaExtractor()
    try {
        extractor.setDataSource(absolutePath)
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (mime?.startsWith("audio/") == true) {
                val durationUs = format.getLong(MediaFormat.KEY_DURATION)
                return durationUs / 1000L
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return -1
}