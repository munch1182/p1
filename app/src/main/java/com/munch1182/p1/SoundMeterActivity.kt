package com.munch1182.p1

import android.Manifest
import android.content.Intent
import android.media.AudioFormat
import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.munch1182.lib.AppHelper
import com.munch1182.lib.base.toByteArray
import com.munch1182.lib.helper.FileHelper
import com.munch1182.lib.other.RecordManager
import com.munch1182.lib.other.calculateDB
import com.munch1182.lib.result.intent
import com.munch1182.lib.result.isAllGrant
import com.munch1182.lib.result.permission
import com.munch1182.p1.ui.ButtonDefault
import com.munch1182.p1.ui.CheckBoxLabel
import com.munch1182.p1.ui.Split
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.FileOutputStream

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
        Split()
        //FileView()
    }

    @Composable
    fun FileView() {
        val path by vm.path.observeAsState()

        path?.let {
            if (it.success) {
                Text("已保存文件到: ${it.realPath}, 点击分享", modifier = Modifier.clickable {
                    intent(Intent(Intent.ACTION_SEND).apply {
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        setDataAndType(it.uri!!, AppHelper.contentResolver.getType(it.uri))
                        putExtra(Intent.EXTRA_STREAM, it.uri)
                    }.let { i -> Intent.createChooser(i, "share") }).request {}
                })
            } else {
                Text("文件保存失败")
            }
        }
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
        var file by remember { mutableStateOf(false) }

        CheckBoxLabel("保存到文件", file, { file = it })

        ButtonDefault(if (isRecording) "停止录音" else "开始录音") {
            permission(Manifest.permission.RECORD_AUDIO).request {
                if (!it.isAllGrant()) return@request
                lifecycleScope.launch(Dispatchers.IO) { vm.toggle(file) }
            }
        }

        Split()
        Text("分贝：${meterValue.soundMete}")
        Split()
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
    private var _path = MutableLiveData<PathResult?>(null)

    val smsv: LiveData<SoundMeterShowValue> = _smsv
    val isRecording: LiveData<Boolean> = _isRecording
    val meterValue: StateFlow<MeterValue> = _meterValue.asStateFlow()
    val path: LiveData<PathResult?> = _path

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

    suspend fun startRecord(file: Boolean = false) {
        if (sm.sampleRate != smsvRef.sampleRate || sm.channel != smsvRef.channel) {
            sm.release()
            sm = RecordManager(smsvRef.sampleRate, smsvRef.channel)
        }
        list.clear()
        sm.start()
        _isRecording.postValue(sm.isRecording)


        val name = "record.pcm"
        val (f, fos) = if (file) FileHelper.newCacheFile(name).let { it to FileOutputStream(it) } else null to null
        while (sm.isRecording) {
            delay(100L)
            val (bytes, read) = sm.readWithReadValue() ?: continue
            val db = bytes.calculateDB(read)

            fos?.write(bytes.toByteArray())

            if (list.size >= avgSize) {
                list.sort()
                meterValueRef.min = list.first()
                meterValueRef.max = list.last()
                meterValueRef.avg = list[avgSize / 2] // 中间值
                list.clear()
            }
            list.add(db)
            meterValueRef.soundMete = db
            _meterValue.emit(meterValueRef.new())
        }
        runCatching {
            fos?.flush()
            fos?.close()
        }
        if (file) {
            if (f != null) {
                _path.postValue(PathResult(true, Uri.fromFile(f), f.path))
            } else {
                _path.postValue(PathResult())
            }
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

    suspend fun toggle(file: Boolean = false) = if (sm.isRecording) stopRecord() else startRecord(file)

    class SoundMeterShowValue(var sampleRate: Int = 44100, var channel: Int = AudioFormat.CHANNEL_IN_MONO) {
        constructor(v: SoundMeterShowValue) : this(v.sampleRate, v.channel)

        fun new() = SoundMeterShowValue(this)
    }

    class MeterValue(var soundMete: Double = 0.0, var min: Double = 0.0, var max: Double = 0.0, var avg: Double = 0.0) {
        constructor(v: MeterValue) : this(v.soundMete, v.min, v.max, v.avg)

        fun new() = MeterValue(this)
    }

    class PathResult(val success: Boolean = false, val uri: Uri? = null, val realPath: String? = null)
}



