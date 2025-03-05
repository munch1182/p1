package com.munch1182.p1

import android.Manifest
import android.media.AudioFormat
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.coroutineScope
import com.munch1182.lib.PermissionHelper
import com.munch1182.lib.appDetailsPage
import com.munch1182.lib.other.SoundMeter
import com.munch1182.p1.ui.theme.P1Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SoundMeterActivity : FragmentActivity() {
    private var sm = SoundMeter()

    // 注册权限回调
    private val rfa = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (!it) {
            startActivity(appDetailsPage)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContentWithBase { SoundMeterView() }
    }

    override fun onDestroy() {
        super.onDestroy()
        sm.release()
    }

    data class MeterValue(var min: Double, var max: Double, var avg: Double)

    @Composable
    fun SoundMeterView() {

        var sampleRate by remember { mutableIntStateOf(44100) }
        var channel by remember { mutableIntStateOf(AudioFormat.CHANNEL_IN_STEREO) }
        var soundMete by remember { mutableDoubleStateOf(0.0) }
        var isRecording by remember { mutableStateOf(sm.isRecording) }
        val meterValue by remember { mutableStateOf(MeterValue(0.0, 0.0, 0.0)) }

        val avgSize = 100
        val list = ArrayList<Double>(avgSize)

        Button({
            sm.stop()
            isRecording = sm.isRecording
            sampleRate = when (sampleRate) {
                44100 -> 48000
                48000 -> 96000
                96000 -> 8000
                8000 -> 16000
                16000 -> 22050
                else -> 44100
            }
        }) { Text("切换采样率: $sampleRate Hz") }
        Button({
            sm.stop()
            isRecording = sm.isRecording
            channel =
                if (channel == AudioFormat.CHANNEL_IN_MONO) AudioFormat.CHANNEL_IN_STEREO else AudioFormat.CHANNEL_IN_MONO
        }) { Text("切换声道: ${if (channel == AudioFormat.CHANNEL_IN_MONO) "单声道" else "双声道"}") }
        Spacer(Modifier.height(16.dp))
        Button({
            if (!PermissionHelper.check(Manifest.permission.RECORD_AUDIO)) {
                rfa.launch(Manifest.permission.RECORD_AUDIO)
                return@Button
            }
            if (sm.sampleRate != sampleRate || sm.channel != channel) {
                sm.release()
                sm = SoundMeter(sampleRate, channel)
            }
            if (!sm.isRecording) {
                list.clear()
                sm.start()
                lifecycle.coroutineScope.launch(Dispatchers.IO) {
                    while (sm.isRecording) {
                        delay(100)
                        soundMete = sm.updateAmplitude()
                        if (list.size == avgSize) {
                            meterValue.apply {
                                list.sort()
                                min = list.first()
                                max = list.last()
                                meterValue.avg = list[avgSize / 2]
                            }

                            list.clear()
                        }
                        list.add(soundMete)
                    }
                }
            } else {
                sm.stop()
            }
            isRecording = sm.isRecording
        }) { Text(if (isRecording) "停止录音" else "开始录音") }
        Spacer(Modifier.height(16.dp))
        Text("分贝：$soundMete")
        Spacer(Modifier.height(16.dp))
        Text("最低: ${meterValue.min}")
        Text("中位: ${meterValue.avg}")
        Text("最高: ${meterValue.max}")
    }

    @Preview
    @Composable
    fun SoundMeterViewPreview() {
        P1Theme { SoundMeterView() }
    }
}


