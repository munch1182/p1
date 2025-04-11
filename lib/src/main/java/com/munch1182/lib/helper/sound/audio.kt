package com.munch1182.lib.helper.sound

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import androidx.lifecycle.LifecycleOwner
import com.munch1182.lib.base.log
import com.munch1182.lib.base.onDestroyed

/**
 * 边写边播的音频播放
 */
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
    val buffSize = AudioTrack.getMinBufferSize(sampleRate, channel, format) * 2
    private val player = AudioTrack.Builder().setAudioAttributes(
        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
    ).setAudioFormat(audioFormat).setBufferSizeInBytes(buffSize).setTransferMode(AudioTrack.MODE_STREAM) // 边写边播
        .build()

    val state get() = player.playState

    val newBuffer: ByteArray get() = ByteArray(player.bufferCapacityInFrames)

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

    private var writeListener: (() -> Unit)? = null

    private val callback = object : AudioTrack.OnPlaybackPositionUpdateListener {
        override fun onMarkerReached(track: AudioTrack?) {
            log.logStr("onMarkerReached: ${track?.notificationMarkerPosition}, ${track?.bufferSizeInFrames}")
            writeListener?.invoke()
            writeListener = null
        }

        override fun onPeriodicNotification(track: AudioTrack?) {
        }
    }

    fun prepare() {
        if (state == AudioTrack.PLAYSTATE_PLAYING) return
        log.logStr("prepare")
        player.play()
        player.setPlaybackPositionUpdateListener(callback)
    }

    var writed = 0

    fun write(data: ByteArray, start: Int = 0, len: Int = data.size) {
        player.write(data, start, len)
        writed += (len / fsib)
    }

    fun writeOver() {
        //suspendCoroutine { c -> write(data, start, len) { c.resume(Unit) } }
        player.setNotificationMarkerPosition(writed / fsib)
    }

    fun stop() {
        log.logStr("stop")
        player.stop()
    }

    fun release() {
        log.logStr("release")
        player.pause()
        player.release()
    }

}