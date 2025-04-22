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

    /**
     * @see newBuffer
     */
    val buffSize = AudioTrack.getMinBufferSize(sampleRate, channel, format) * 2

    private val player = AudioTrack.Builder().setAudioAttributes(
        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
    ).setAudioFormat(audioFormat).setBufferSizeInBytes(buffSize)
        .setTransferMode(AudioTrack.MODE_STREAM) // 边写边播
        .build()

    val state get() = player.playState

    val newBuffer: ByteArray get() = ByteArray(player.bufferCapacityInFrames)

    private val fsib by lazy { audioFormat.frameSizeInBytesCompat }

    private var writeListener: (() -> Unit)? = null

    private val callback = object : AudioTrack.OnPlaybackPositionUpdateListener {
        override fun onMarkerReached(track: AudioTrack?) {
            log.logStr("onMarkerReached: pos: ${track?.notificationMarkerPosition}, written: $written")
            writeListener?.invoke()
            writeListener = null
        }

        override fun onPeriodicNotification(track: AudioTrack?) {
        }
    }

    fun prepare() {
        log.logStr("prepare")
        written = 0
        if (state == AudioTrack.PLAYSTATE_PLAYING) return
        player.play()
        player.setPlaybackPositionUpdateListener(callback)
    }

    private var written = 0

    /**
     * 需要保证线程安全，否则多线程同时写入会导致播放错误，也可能会导致原生方法的缓存溢出而抛出崩溃
     * 因为要循环多次调用该方法，所以线程安全交由外部实现
     *
     * @see prepare
     * @see writeOver
     * @see stop
     */
    fun write(data: ByteArray, start: Int = 0, len: Int = data.size) {
        player.write(data, start, len)
        written += (len / fsib)
    }

    /**
     * 写完后调用，回调播放完成
     */
    fun writeOver(l: (() -> Unit)? = null) {
        log.logStr("writeOver: written: $written")

        this.writeListener = l
        player.setNotificationMarkerPosition(written)
    }

    fun stop() {
        log.logStr("stop")
        player.stop()
    }

    fun release() {
        log.logStr("release")
        stop()
        player.release()
    }
}

val AudioFormat.frameSizeInBytesCompat
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        frameSizeInBytes
    } else {
        // see android.media.AudioFormat#getBytesPerSample
        when (encoding) {
            AudioFormat.ENCODING_PCM_8BIT -> 1
            AudioFormat.ENCODING_PCM_16BIT, AudioFormat.ENCODING_IEC61937, AudioFormat.ENCODING_DEFAULT -> 2
            AudioFormat.ENCODING_PCM_24BIT_PACKED -> 3
            AudioFormat.ENCODING_PCM_FLOAT, AudioFormat.ENCODING_PCM_32BIT -> 4
            else -> 1
        } * channelCount
    }