package com.munch1182.lib.helper.sound

import android.content.Context
import android.content.Intent
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.session.MediaSession
import android.os.Build
import android.os.Handler
import android.view.KeyEvent
import androidx.annotation.RequiresApi
import com.munch1182.lib.AppHelper
import com.munch1182.lib.base.OnUpdateListener
import com.munch1182.lib.base.ThreadHelper
import com.munch1182.lib.base.ctx
import com.munch1182.lib.base.getParcelableCompat
import com.munch1182.lib.base.log
import com.munch1182.lib.helper.ARDefaultManager
import com.munch1182.lib.helper.ARManager
import java.util.concurrent.Executor

object AudioHelper {

    val am get() = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager

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
    }

    class MediaButtonHelper : ARManager<OnUpdateListener<KeyEvent>> by ARDefaultManager() {

        companion object {
            private const val TAG = "com.munch1182.lib.helper.sound.MediaButtonHelper.MediaSession"
        }

        private val log = log()
        private var session: MediaSession? = null

        private val callback = object : MediaSession.Callback() {
            override fun onMediaButtonEvent(mediaButtonIntent: Intent): Boolean {
                val key = mediaButtonIntent.extras?.getParcelableCompat<KeyEvent>(Intent.EXTRA_KEY_EVENT) ?: return super.onMediaButtonEvent(mediaButtonIntent)
                log.logStr("${key.keyCode} ${key.action} ${key.eventTime}")
                forEach { it.onUpdate(key) }
                return super.onMediaButtonEvent(mediaButtonIntent)
            }
        }

        /**
         * 要让系统分发按键事件：
         * 1. 获取播放焦点；否则其它播放器将响应按键
         * 2. 要有实际的播放过的播放器(不必正在播放，非必须响应按键)；否则当其它播放器播放时仍会响应按键
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
            session?.release()
            session = null
        }

        private fun newSession(): MediaSession {
            if (session != null) return session!!
            val session = MediaSession(AppHelper, TAG)
            session.setCallback(callback)
            this.session = session
            return session
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    class InputHelper {

        fun listenChanged(executor: Executor = ThreadHelper.cacheExecutor, listener: AudioManager.OnCommunicationDeviceChangedListener) {
            am.addOnCommunicationDeviceChangedListener(executor, listener)
        }

        fun unListenChanged(listener: AudioManager.OnCommunicationDeviceChangedListener) {
            am.removeOnCommunicationDeviceChangedListener(listener)
        }

        /**
         * @see android.media.AudioDeviceInfo.TYPE_BLUETOOTH_SCO
         *
         * 打开蓝牙的sco通道，即可通过蓝牙设备录音
         * 但是此时蓝牙设备按键大多数会自动关闭该通道(形同蓝牙耳机按键挂断通话)
         */
        fun setRecordFrom(type: Int): Boolean? {
            if (am.communicationDevice?.type == type) return true
            return am.availableCommunicationDevices.firstOrNull { it.type == type }?.let { am.setCommunicationDevice(it) }
        }

        fun resetRecordFrom() {
            am.clearCommunicationDevice()
        }

        fun currRecordFrom(): Int? = am.communicationDevice?.type
    }
}