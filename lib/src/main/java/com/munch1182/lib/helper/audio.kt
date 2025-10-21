package com.munch1182.lib.helper

import android.Manifest
import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import com.munch1182.lib.base.Releasable
import com.munch1182.lib.base.log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.min

class RecordHelper(
    val sampleRateInHz: Int = 44100, // 采样率，44100Hz兼容
    val channelConfig: Int = AudioFormat.CHANNEL_IN_MONO, // 单声道双声道
    val audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT, // 数据位宽，16BIT兼容
) : Releasable {

    companion object {

        /**
         * 音频识别
         */
        fun recognition() = RecordHelper(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
    }

    private val log = log()
    private val record by lazy { newRecord() }
    val bufferSize by lazy { AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat) }

    @SuppressLint("MissingPermission")
    private fun newRecord() = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRateInHz, channelConfig, audioFormat, bufferSize)

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun record(time: Long = 40L) = callbackFlow {
        log.logStr("start record")
        record.startRecording()
        val maxSize = bufferSize
        val buff = ByteArray(maxSize)
        try {
            while (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                delay(time)
                val size = record.read(buff, 0, maxSize)
                when {
                    size > 0 -> send(buff.copyOfRange(0, min(size, maxSize)))

                    size == AudioRecord.ERROR_INVALID_OPERATION -> {
                        close(IllegalStateException("ERROR_INVALID_OPERATION"))
                        break
                    }

                    size == AudioRecord.ERROR_BAD_VALUE -> {
                        close(IllegalStateException("ERROR_BAD_VALUE"))
                        break
                    }
                }
            }
        } catch (e: Exception) {
            close(e)
        } finally {
            log.logStr("finally, stop record")
            if (record.state == AudioRecord.STATE_INITIALIZED) record.stop()
        }
        awaitClose {}
    }

    override fun release() {
        record.release()
    }
}