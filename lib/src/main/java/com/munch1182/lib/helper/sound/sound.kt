package com.munch1182.lib.helper.sound

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.Handler
import android.view.KeyEvent
import com.munch1182.lib.AppHelper
import com.munch1182.lib.base.OnUpdateListener
import com.munch1182.lib.base.ThreadHelper
import com.munch1182.lib.base.ctx
import com.munch1182.lib.base.getParcelableCompat
import com.munch1182.lib.base.log
import com.munch1182.lib.helper.ARDefaultManager
import com.munch1182.lib.helper.ARManager
import com.munch1182.lib.helper.BaseReceiver
import java.util.concurrent.Executor

object AudioHelper {

    val am get() = ctx.getSystemService(AudioManager::class.java)

    class FocusHelper : ARManager<AudioManager.OnAudioFocusChangeListener> by ARDefaultManager() {

        private var focusGain = AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE

        private val callback = AudioManager.OnAudioFocusChangeListener { focus ->
            forEach { it.onAudioFocusChange(focus) }
        }

        /**
         * @see AudioManager.AUDIOFOCUS_GAIN 打断其它播放
         * @see AudioManager.AUDIOFOCUS_GAIN_TRANSIENT 打断其它播放，清除后恢复其它播放(如果其它播放支持)
         * @see AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK 其它播放声音变小作为背景音，清除后声音恢复
         * @see AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE 打断其它播放，清除后恢复其它播放；请求期间系统不应该播放任何通知，用于语音录制或者识别
         */
        fun setFocusGain(focusGain: Int): FocusHelper {
            this.focusGain = focusGain
            return this
        }

        fun requestAudioFocus(handler: Handler? = null): Boolean {
            return am.requestAudioFocus(
                AudioFocusRequest.Builder(focusGain).apply {
                    handler?.let { setOnAudioFocusChangeListener(callback, handler) } ?: setOnAudioFocusChangeListener(callback)
                }.build()
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }

        fun clearAudioFocus(): Boolean {
            return am.abandonAudioFocusRequest(AudioFocusRequest.Builder(focusGain).setOnAudioFocusChangeListener(callback).build()) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }

        fun release() {
            clear()
            clearAudioFocus()
        }
    }

    class MediaButtonHelper(private var isOrigin: Boolean = false) : ARManager<OnUpdateListener<KeyEvent>> by ARDefaultManager() {

        companion object {
            private const val TAG = "com.munch1182.lib.helper.sound.MediaButtonHelper.MediaSession"
        }

        private val log = log()
        private var session: MediaSession? = null

        private val callback = object : MediaSession.Callback() {
            private var currKey: KeyEvent? = null
            override fun onMediaButtonEvent(mediaButtonIntent: Intent): Boolean {
                val key = mediaButtonIntent.extras?.getParcelableCompat<KeyEvent>(Intent.EXTRA_KEY_EVENT) ?: return super.onMediaButtonEvent(mediaButtonIntent)
                log.logStr("${key.keyCode} ${key.action} ${key.eventTime}")
                if (isOrigin) forEach { it.onUpdate(key) }
                currKey = key
                super.onMediaButtonEvent(mediaButtonIntent)
                return true
            }

            override fun onPause() {
                super.onPause()
                log.logStr("on: $currKey")
                val key = currKey ?: return
                if (!isOrigin) forEach { it.onUpdate(key) }
            }

            override fun onPlay() {
                super.onPlay()
                val key = currKey ?: return
                if (!isOrigin) forEach { it.onUpdate(key) }
            }

            override fun onSkipToNext() {
                super.onSkipToNext()
                val key = currKey ?: return
                if (!isOrigin) forEach { it.onUpdate(key) }
            }

            override fun onSkipToPrevious() {
                super.onSkipToPrevious()
                val key = currKey ?: return
                if (!isOrigin) forEach { it.onUpdate(key) }
            }
        }

        fun isOriginKeyEvent(isOrigin: Boolean = false): MediaButtonHelper {
            this.isOrigin = isOrigin
            return this
        }

        /**
         * 要让系统分发按键事件：
         * 要作为最后一个运行过的播放器
         *
         * @see MediaSession.Callback.onMediaButtonEvent
         * @see callback
         */
        fun listen() {
            newSession().isActive = true
        }

        fun unListen() {
            session?.isActive = false
        }

        fun release() {
            clear()
            unListen()
            session?.release()
            session = null
        }

        private fun newSession(): MediaSession {
            if (session != null) return session!!
            val session = MediaSession(AppHelper, TAG)
            session.setCallback(callback)
            session.setPlaybackState(
                PlaybackState.Builder().setActions(
                    PlaybackState.ACTION_PLAY or
                            PlaybackState.ACTION_PAUSE or
                            PlaybackState.ACTION_PLAY_PAUSE or
                            PlaybackState.ACTION_SKIP_TO_NEXT or
                            PlaybackState.ACTION_SKIP_TO_PREVIOUS
                ).build()
            )
            this.session = session
            return session
        }
    }

    class BlueScoHelper {
        private val communicateListener = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AudioManager.OnCommunicationDeviceChangedListener { scoListener?.onUpdate(it) }
        } else {
            null
        }
        private var scoListener: OnUpdateListener<AudioDeviceInfo?>? = null

        fun addBlueDeviceChangeListener(executor: Executor = ThreadHelper.cacheExecutor, l: OnUpdateListener<AudioDeviceInfo?>) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                am.addOnCommunicationDeviceChangedListener(executor, communicateListener!!)
            }
            this.scoListener = l
        }

        fun removeBlueDeviceChangeListener() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                am.removeOnCommunicationDeviceChangedListener(communicateListener!!)
            }
            scoListener = null
        }

        @Suppress("DEPRECATION")
        fun startBlueSco(): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                am.availableCommunicationDevices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO }?.let { am.setCommunicationDevice(it) } == true
            } else {
                am.startBluetoothSco()
                true
            }
        }

        @Suppress("DEPRECATION")
        fun isBlueScoOn(): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                am.communicationDevice?.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
            } else am.isBluetoothScoOn
        }

        @Suppress("DEPRECATION")
        fun stopBlueSco() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                am.clearCommunicationDevice()
            } else {
                am.isSpeakerphoneOn = false
                am.stopBluetoothSco()
            }
        }
    }

    class BlueScoAudioReceiver : BaseReceiver<OnUpdateListener<Int>>(IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)) {
        private val log = log()
        override fun onReceive(context: Context?, intent: Intent?) {
            val state = intent?.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1)
            log.logStr("${intent?.action}: $state")
            state ?: return
            dispatchOnReceive { it.onUpdate(state) }
        }
    }
}