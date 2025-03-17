package com.munch1182.p1

import android.Manifest
import android.media.AudioFormat
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.munch1182.lib.other.RecordManager
import com.munch1182.lib.other.calculateDB
import com.munch1182.lib.result.isAllGrant
import com.munch1182.lib.result.permission
import com.munch1182.p1.ui.ButtonDefault
import com.munch1182.p1.ui.Split
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SoundMeterActivity : FragmentActivity() {

    private val vm by viewModels<SoundMeterVM>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentWithBase { SoundMeterView() }
    }


    @Composable
    fun SoundMeterView() {
        ManageView()
        Split()
        RecordView()
    }

    @Composable
    fun ManageView() {
        // 不能使用dataclass类
        val smsv by vm.smsv.observeAsState(SoundMeterVM.SoundMeterShowValue())

        ButtonDefault("切换采样率: ${smsv.sampleRate} Hz") {
            vm.stopRecord()
            vm.nextTypeSampleRate()
        }

        ButtonDefault("切换声道: ${if (smsv.channel == AudioFormat.CHANNEL_IN_MONO) "单声道" else "双声道"}") {
            vm.stopRecord()
            vm.nextChannel()
        }
    }


    @Composable
    fun RecordView() {
        val isRecording by vm.isRecording.observeAsState(false)
        val meterValue by vm.meterValue.collectAsStateWithLifecycle()

        ButtonDefault(if (isRecording) "停止录音" else "开始录音") {
            permission(Manifest.permission.RECORD_AUDIO).request {
                if (!it.isAllGrant()) return@request
                lifecycleScope.launch(Dispatchers.IO) { vm.toggle() }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("分贝：${meterValue.soundMete}")
        Spacer(Modifier.height(16.dp))
        Text("最低: ${meterValue.min}")
        Text("中位: ${meterValue.avg}")
        Text("最高: ${meterValue.max}")
    }
}

class SoundMeterVM : ViewModel() {

    private var sm = RecordManager()

    private val smsvRef = SoundMeterShowValue()
    private val _smsv = MutableLiveData(smsvRef)
    private var _isRecording = MutableLiveData(false)
    private val meterValueRef = MeterValue()
    private var _meterValue = MutableStateFlow(meterValueRef)

    val smsv: LiveData<SoundMeterShowValue> = _smsv
    val isRecording: LiveData<Boolean> = _isRecording
    val meterValue: StateFlow<MeterValue> = _meterValue.asStateFlow()

    private val avgSize = 100
    private val list = ArrayList<Double>(avgSize)

    override fun onCleared() {
        super.onCleared()
        sm.release()
    }

    fun stopRecord() {
        sm.stop()
        _isRecording.postValue(sm.isRecording)
    }

    suspend fun startRecord() {
        if (sm.sampleRate != smsvRef.sampleRate || sm.channel != smsvRef.channel) {
            sm.release()
            sm = RecordManager(smsvRef.sampleRate, smsvRef.channel)
        }
        list.clear()
        sm.start()
        _isRecording.postValue(sm.isRecording)

        while (sm.isRecording) {
            delay(100L)
            val read = sm.readWithReadValue()
            val soundMete = read?.let { it.first.calculateDB(it.second) } ?: 0.0

            if (list.size >= avgSize) {
                list.sort()
                meterValueRef.min = list.first()
                meterValueRef.max = list.last()
                meterValueRef.avg = list[avgSize / 2] // 中间值
                list.clear()
            }
            list.add(soundMete)
            meterValueRef.soundMete = soundMete
            _meterValue.emit(meterValueRef.new())
        }
    }


    private fun newSM(new: SoundMeterShowValue.() -> Unit) {
        new(smsvRef)
        _smsv.postValue(smsvRef.new())
        _isRecording.postValue(sm.isRecording)
    }

    fun nextChannel() {
        newSM {
            channel = if (channel == AudioFormat.CHANNEL_IN_MONO) AudioFormat.CHANNEL_IN_STEREO else AudioFormat.CHANNEL_IN_MONO
        }
    }

    fun nextTypeSampleRate() {
        newSM {
            sampleRate = when (sampleRate) {
                44100 -> 48000
                48000 -> 96000
                96000 -> 8000
                8000 -> 16000
                16000 -> 22050
                else -> 44100
            }
        }
    }

    suspend fun toggle() = if (sm.isRecording) stopRecord() else startRecord()

    class SoundMeterShowValue(var sampleRate: Int = 44100, var channel: Int = AudioFormat.CHANNEL_IN_MONO) {
        constructor(v: SoundMeterShowValue) : this(v.sampleRate, v.channel)

        fun new() = SoundMeterShowValue(this)
    }

    class MeterValue(var soundMete: Double = 0.0, var min: Double = 0.0, var max: Double = 0.0, var avg: Double = 0.0) {
        constructor(v: MeterValue) : this(v.soundMete, v.min, v.max, v.avg)

        fun new() = MeterValue(this)
    }
}



