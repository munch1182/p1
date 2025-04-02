package com.munch1182.lib.helper.sound

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.lifecycle.LifecycleOwner
import com.munch1182.lib.base.log
import com.munch1182.lib.base.onDestroyed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import java.util.concurrent.LinkedBlockingQueue

class AudioPlayer(
    val sampleRate: Int = 44100, val channel: Int = AudioFormat.CHANNEL_OUT_MONO
) {

    constructor(owner: LifecycleOwner, sampleRate: Int = 44100, channel: Int = AudioFormat.CHANNEL_IN_STEREO) : this(sampleRate, channel) {
        owner.lifecycle.onDestroyed { release() }
    }

    private val log = log()
    val buffSize = AudioTrack.getMinBufferSize(sampleRate, channel, AudioFormat.ENCODING_PCM_16BIT)
    private val player = AudioTrack.Builder().setAudioAttributes(
        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
    ).setAudioFormat(
        AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(sampleRate).setChannelMask(channel).build()
    ).setBufferSizeInBytes(buffSize).setTransferMode(AudioTrack.MODE_STREAM) // 边写边播
        .build()
    private val queue = LinkedBlockingQueue<Data>()
    private var job: Job? = null

    fun write(data: ByteArray, start: Int = 0, len: Int = data.size) {
        log.logStr("write: ${queue.size}")
        queue.put(Data(data, start, len))
    }

    suspend fun prepare() {
        if (job != null) return
        job = Job()
        log.logStr("prepare()")
        withContext(job!! + Dispatchers.IO) {
            log.logStr("play")
            player.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onMarkerReached(track: AudioTrack?) {
                    log.logStr("onMarkerReached  ${track?.playbackHeadPosition}")
                }

                override fun onPeriodicNotification(track: AudioTrack?) {
                }
            })
            player.play()
            while (true) {
                log.logStr("read: ${queue.size}")
                val data = queue.take()
                if (data.len == 0) {
                    log.logStr("stop")
                    break
                }
                val write = player.write(data.byte, data.start, data.len)
                log.logStr("write data: ${data.len}, $write")
                player.setNotificationMarkerPosition(data.len / 2)
            }
        }
        log.logStr("play over")
        job = null
    }

    fun stopNow() {
        log.logStr("stopNow()")
        job?.cancel()
        player.stop()
        queue.clear()
    }

    fun stop() {
        log.logStr("stop()")
        queue.put(Data(byteArrayOf()))
    }

    fun write(data: String) = write(data.toByteArray())

    fun release() {
        job?.cancel()
        job = null
        player.stop()
        kotlin.runCatching { player.release() }
    }

    class Data(val byte: ByteArray, val start: Int = 0, val len: Int = byte.size)
}