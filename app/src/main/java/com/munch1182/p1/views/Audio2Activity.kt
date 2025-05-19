package com.munch1182.p1.views

import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.munch1182.lib.AppHelper
import com.munch1182.lib.base.asLive
import com.munch1182.lib.base.asStateFlow
import com.munch1182.lib.base.launchIO
import com.munch1182.lib.base.log
import com.munch1182.lib.base.onDestroyed
import com.munch1182.lib.base.toDateStr
import com.munch1182.lib.helper.sound.AudioHelper
import com.munch1182.p1.R
import com.munch1182.p1.base.BaseActivity
import com.munch1182.p1.base.DialogHelper
import com.munch1182.p1.base.show
import com.munch1182.p1.base.toast
import com.munch1182.p1.ui.ClickButton
import com.munch1182.p1.ui.ComposeListView
import com.munch1182.p1.ui.ComposeView
import com.munch1182.p1.ui.DownUpArrow
import com.munch1182.p1.ui.EmptyMsg
import com.munch1182.p1.ui.Split
import com.munch1182.p1.ui.StateButton
import com.munch1182.p1.ui.setContentWithRv
import com.munch1182.p1.ui.theme.PagePadding
import com.munch1182.p1.ui.theme.PagePaddingHalf
import kotlinx.coroutines.flow.MutableStateFlow

class Audio2Activity : BaseActivity() {

    private val audioVM by viewModels<Audio2VM>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentWithRv { Views() }
    }

    @Composable
    private fun Views() {
        val isFocus by audioVM.focus.observeAsState(false)
        val isListen by audioVM.isListen.observeAsState(false)
        var showMbv by remember { mutableStateOf(false) }
        StateButton(if (isFocus) "清除当前焦点" else "获取音频焦点", isFocus) { audioVM.gainFocusOrNot(!isFocus) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            StateButton(if (isListen) "取消按键监听" else "监听媒体按键", isListen) { audioVM.listenButtonOrNot(!isListen) }
            Spacer(Modifier.width(PagePadding))
            if (isListen) DownUpArrow(showMbv) { showListenMediaButtonView { showMbv = it } }
        }

        Split()
        ClickButton("输入设备") { showSelectDevicesView(audioVM.collectInputDevices(), null) {} }
        ClickButton("输出设备") {
            showSelectDevicesView(audioVM.collectOutputDevices(), null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    audioVM.setPreferredDevice(it) //实时切换的，除了蓝牙SCO
                } else {
                    toast("版本低于${Build.VERSION_CODES.P}, 无法使用")
                }
            }
        }
        Split()

        StateButton("开始录音", false) {
            audioVM.playOrStop()
        }
    }

    private fun showSelectDevicesView(devs: Array<out AudioDeviceInfo>?, routed: AudioDeviceInfo?, chose: (AudioDeviceInfo?) -> Unit) {
        devs ?: return
        DialogHelper.newBottom { ctx, d ->
            ComposeListView(ctx, devs.size, Modifier.padding(vertical = PagePadding)) {
                val dev = devs[it]
                if (it == 0) HorizontalDivider()
                Column(
                    modifier = Modifier.clickable {
                        d?.cancel()
                        chose.invoke(dev)
                    }) {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = PagePadding, vertical = PagePaddingHalf)
                            .fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(dev.type.toAudiStr(), fontWeight = FontWeight.ExtraBold)
                        Text(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) "(${dev.address}, ${dev.productName})" else "(${dev.productName})")
                        Text(if (dev.sampleRates.isEmpty() || dev.sampleRates.size == 9) "*" else dev.sampleRates.joinToString("/"))
                        Text(if (dev.channelMasks.isEmpty()) "*" else dev.channelMasks.joinToString { i -> i.toChannelStr() })
                        Text(if (dev.encodings.isEmpty()) "*" else dev.encodings.joinToString { i -> i.toEncodingStr() })
                        if (routed?.type != null && dev.type == routed.type) Text("ROUTED")
                    }
                    HorizontalDivider()
                }
            }
        }.show()
    }

    private fun showListenMediaButtonView(showOrNot: (Boolean) -> Unit) {
        val list = mutableListOf<Pair<KeyEvent, Long>>()
        DialogHelper.newBottom { ctx, _ ->
            ComposeView(ctx) {
                val btn by audioVM.mediaButton.collectAsState()
                btn?.let { list.add(it to System.currentTimeMillis()) }

                EmptyMsg(list.isEmpty(), "当前未接收到按键") {
                    LazyColumn(modifier = Modifier.defaultMinSize(minHeight = 200.dp)) {
                        items(list.size) { i ->
                            val (it, time) = list[i]
                            Text("${it.toStr()} ${time.toDateStr("mm:ss.sss")}", modifier = Modifier.padding(horizontal = PagePadding, vertical = PagePaddingHalf))
                        }
                    }
                }
            }
        }.apply { lifecycle.onDestroyed { showOrNot.invoke(false) } }.show()
        showOrNot.invoke(true)
    }

    private fun Int.toChannelStr(): String {
        return when (this) {
            AudioFormat.CHANNEL_IN_MONO -> "CHANNEL_IN_MONO"
            AudioFormat.CHANNEL_IN_STEREO -> "CHANNEL_IN_STEREO"
            AudioFormat.CHANNEL_IN_BACK -> "CHANNEL_IN_BACK"
            AudioFormat.CHANNEL_IN_LEFT -> "CHANNEL_IN_FRONT_BACK"
            AudioFormat.CHANNEL_IN_RIGHT -> "CHANNEL_IN_SIDE"
            else -> this.toString()
        }
    }

    private fun Int.toEncodingStr(): String {
        return when (this) {
            AudioFormat.ENCODING_PCM_8BIT -> "ENCODING_PCM_8BIT"
            AudioFormat.ENCODING_PCM_16BIT -> "ENCODING_PCM_16BIT"
            AudioFormat.ENCODING_PCM_FLOAT -> "ENCODING_PCM_FLOAT"
            AudioFormat.ENCODING_AC3 -> "ENCODING_AC3"
            AudioFormat.ENCODING_E_AC3 -> "ENCODING_E_AC3"
            AudioFormat.ENCODING_DTS -> "ENCODING_DTS"
            AudioFormat.ENCODING_DTS_HD -> "ENCODING_DTS_HD"
            AudioFormat.ENCODING_IEC61937 -> "ENCODING_IEC61937"
            else -> this.toString()
        }
    }

    private fun KeyEvent.toStr(): String {
        val action = when (action) {
            KeyEvent.ACTION_DOWN -> "按下"
            KeyEvent.ACTION_UP -> "抬起"
            else -> action.toString()
        }
        val name = when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY -> "播放"
            KeyEvent.KEYCODE_MEDIA_PAUSE -> "暂停"
            KeyEvent.KEYCODE_MEDIA_NEXT -> "下一首"
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> "上一首"
            KeyEvent.KEYCODE_HEADSETHOOK -> "挂断"
            else -> keyCode.toString()
        }
        return "按键：$name: $action"
    }

    private fun Int.toAudiStr(): String {
        val name = when (this) {
            AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "BUILTIN_EARPIECE"
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "BUILTIN_SPEAKER"
            AudioDeviceInfo.TYPE_WIRED_HEADSET -> "WIRED_HEADSET"
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "WIRED_HEADPHONES"
            AudioDeviceInfo.TYPE_LINE_ANALOG -> "WIRED_HEADPHONES"
            AudioDeviceInfo.TYPE_LINE_DIGITAL -> "WIRED_HEADPHONES"
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "BLUETOOTH_SCO"
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "BLUETOOTH_A2DP"
            AudioDeviceInfo.TYPE_BUILTIN_MIC -> "BUILTIN_MIC"
            AudioDeviceInfo.TYPE_FM_TUNER -> "FM_TUNER"
            AudioDeviceInfo.TYPE_TELEPHONY -> "TELEPHONY"
            AudioDeviceInfo.TYPE_USB_HEADSET -> "USB_HEADSET"
            AudioDeviceInfo.TYPE_REMOTE_SUBMIX -> "REMOTE_SUBMIX"
            AudioDeviceInfo.TYPE_BLE_HEADSET -> "BLE_HEADSET"
            AudioDeviceInfo.TYPE_BLE_SPEAKER -> "TYPE_BLE_SPEAKER"

            else -> this.toString()
        }
        return "$name($this)"
    }
}

class Audio2VM : ViewModel() {
    private val log = log()
    private val _focus = MutableLiveData(false)
    private val _mediaButton = MutableStateFlow<KeyEvent?>(null)
    private val _isListen = MutableLiveData(false)

    private val focusHelper = AudioHelper.FocusHelper().setFocusGain(AudioManager.AUDIOFOCUS_GAIN).apply {
        add {
            when (it) {
                AudioManager.AUDIOFOCUS_GAIN -> {
                    log.logStr("Audio Focus Gain")
                    _focus.postValue(true)
                }

                AudioManager.AUDIOFOCUS_LOSS -> {
                    log.logStr("Audio Focus Loss")
                    _focus.postValue(false)
                }

                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    log.logStr("Audio Focus Loss Transient")
                    _focus.postValue(false)
                }
            }
        }
    }
    private val listenHelper = AudioHelper.MediaButtonHelper().apply {
        add {
            log.logStr("按键：${it.keyCode}, ${it.action}")
            viewModelScope.launchIO { _mediaButton.emit(it) }
        }
    }
    private val fakePlayer by lazy { fakePlay() }

    val focus = _focus.asLive()
    val mediaButton = _mediaButton.asStateFlow()
    val isListen = _isListen.asLive()

    override fun onCleared() {
        super.onCleared()
        focusHelper.release()
        listenHelper.release()
        fakePlayer.release()
    }

    fun gainFocusOrNot(focus: Boolean) {
        val result = if (focus) {
            focusHelper.requestAudioFocus()
        } else {
            focusHelper.clearAudioFocus()
        }
        if (result) _focus.postValue(focus)
        log.logStr("gainFocusOrNot: $focus: $result")
    }

    fun listenButtonOrNot(listen: Boolean) {
        if (listen) {
            fakePlayer.start()
            fakePlayer.pause()
            listenHelper.listen()
        } else {
            listenHelper.unListen()
        }
        _isListen.postValue(listen)
        log.logStr("listenButtonOrNot: $listen")
    }

    private fun fakePlay() = MediaPlayer.create(AppHelper, R.raw.lapple).apply { isLooping = true }

    fun collectInputDevices(): Array<out AudioDeviceInfo>? {
        return AudioHelper.am.getDevices(AudioManager.GET_DEVICES_INPUTS)
    }

    fun collectOutputDevices(): Array<out AudioDeviceInfo>? {
        return AudioHelper.am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
    }

    fun setPreferredDevice(dev: AudioDeviceInfo?) {
        if (dev?.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {

        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            fakePlayer.setPreferredDevice(dev)
        }
    }

    fun playOrStop() {
        if (fakePlayer.isPlaying) {
            fakePlayer.pause()
        } else {
            fakePlayer.start()
        }
    }
}