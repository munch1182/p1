package com.munch1182.lib.helper.sound

import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import androidx.annotation.RequiresApi
import com.munch1182.lib.base.ctx

object AudioHelper {

    /**
     * @see android.media.AudioDeviceInfo.TYPE_BLUETOOTH_SCO
     */
    @RequiresApi(Build.VERSION_CODES.S)
    fun setRecordFrom(type: Int) {
        am.availableCommunicationDevices.first { it.type == type }?.let { am.setCommunicationDevice(it) }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun resetRecordFrom() {
        am.clearCommunicationDevice()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun currRecordFrom(): Int? = am.communicationDevice?.type

    val am get() = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    @RequiresApi(Build.VERSION_CODES.O)
    fun requestAudioFocus(handler: Handler? = null, l: AudioManager.OnAudioFocusChangeListener): Int {
        return am.requestAudioFocus(
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).apply {
                handler?.let { setOnAudioFocusChangeListener(l, handler) } ?: setOnAudioFocusChangeListener(l)
            }.build()
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun clearAudioFocus(l: AudioManager.OnAudioFocusChangeListener): Int {
        return am.abandonAudioFocusRequest(AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setOnAudioFocusChangeListener(l).build())
    }
}