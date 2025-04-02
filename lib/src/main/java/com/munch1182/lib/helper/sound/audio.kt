package com.munch1182.lib.helper.sound

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import androidx.lifecycle.LifecycleOwner
import com.munch1182.lib.base.log
import com.munch1182.lib.base.onDestroyed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.util.concurrent.LinkedBlockingQueue

class AudioPlayer(
    val sampleRate: Int = 44100, val channel: Int = AudioFormat.CHANNEL_OUT_MONO, val format: Int = AudioFormat.ENCODING_PCM_16BIT // format必须与音源一致，否则是滋滋声
) {

    constructor(
        owner: LifecycleOwner, sampleRate: Int = 44100, channel: Int = AudioFormat.CHANNEL_IN_STEREO, format: Int = AudioFormat.ENCODING_PCM_16BIT
    ) : this(sampleRate, channel, format) {
        owner.lifecycle.onDestroyed { release() }
    }

    private val log = log()
    private val audioFormat = AudioFormat.Builder().setEncoding(format).setSampleRate(sampleRate).setChannelMask(channel).build()
    val buffSize = AudioTrack.getMinBufferSize(sampleRate, channel, AudioFormat.ENCODING_PCM_8BIT) * 2
    private val player = AudioTrack.Builder().setAudioAttributes(
        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
    ).setAudioFormat(audioFormat).setBufferSizeInBytes(buffSize).setTransferMode(AudioTrack.MODE_STREAM) // 边写边播
        .build()
    private val queue = LinkedBlockingQueue<Data>()
    private var job: Job? = null

    private val fsib by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            audioFormat.frameSizeInBytes
        } else {
            // see android.media.AudioFormat#getBytesPerSample
            when (format) {
                AudioFormat.ENCODING_PCM_8BIT -> 1
                AudioFormat.ENCODING_PCM_16BIT -> 2
                else -> 1
            }
        }
    }

    fun write(data: ByteArray, start: Int = 0, len: Int = data.size) {
        //log.logStr("write: ${queue.size}")
        val byte = ByteArray(len)
        System.arraycopy(data, start, byte, 0, len)
        queue.put(Data(byte))
    }

    fun writeOver() {
        log.logStr("writeOver()")
        queue.put(Data(byteArrayOf()))
    }

    suspend fun prepare() {
        if (job != null) return
        job = Job()
        log.logStr("prepare()")
        withContext(job!! + Dispatchers.IO) {
            log.logStr("play")
            var isOver = false
            player.play()
            player.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onMarkerReached(track: AudioTrack?) {
                    if (isOver) {
                        log.logStr("play over")
                    }
                }

                override fun onPeriodicNotification(track: AudioTrack?) {
                }
            })
            while (coroutineContext.isActive) {
                val data = queue.take()
                if (data.len == 0) {
                    log.logStr("stop")
                    break
                }

                player.write(data.byte, data.start, data.len)
                // 基本上onMarkerReached即播放完成回调，有些微误差不影响数据
                player.setNotificationMarkerPosition(data.len / fsib)
            }
            isOver = true
        }
        log.logStr("loop over")
        job = null
    }

    fun stopNow() {
        log.logStr("stopNow()")
        job?.cancel()
        kotlin.runCatching { player.stop() }
        queue.clear()
    }

    fun write(data: String) = write(data.toByteArray())

    fun release() {
        job?.cancel()
        job = null
        kotlin.runCatching { player.stop() }
        kotlin.runCatching { player.release() }
    }

    class Data(val byte: ByteArray, val start: Int = 0, val len: Int = byte.size)
}