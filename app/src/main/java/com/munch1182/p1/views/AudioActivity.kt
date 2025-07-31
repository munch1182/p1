package com.munch1182.p1.views

import android.content.Intent
import android.media.AudioFormat
import android.os.Build
import android.os.Bundle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import com.munch1182.lib.base.launchIO
import com.munch1182.lib.base.selectDir
import com.munch1182.lib.base.selectFile
import com.munch1182.lib.ffmpeg.FFmpegHelper
import com.munch1182.lib.helper.FileHelper
import com.munch1182.lib.helper.FileHelper.sureExists
import com.munch1182.lib.helper.copyForm
import com.munch1182.lib.helper.result.intent
import com.munch1182.lib.helper.result.requestNow
import com.munch1182.lib.helper.sound.AudioPlayer
import com.munch1182.lib.helper.sound.write
import com.munch1182.p1.base.BaseActivity
import com.munch1182.p1.base.DialogHelper
import com.munch1182.p1.ui.ClickButton
import com.munch1182.p1.ui.SplitV
import com.munch1182.p1.ui.StateButton
import com.munch1182.p1.ui.setContentWithRv
import java.io.File
import java.io.FileInputStream

class AudioActivity : BaseActivity() {

    private var currPlayer: AudioPlayer? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentWithRv { Views() }
    }

    @Composable
    private fun Views() {
        var name by remember { mutableStateOf("") }
        var info by remember { mutableStateOf(AudioInfo()) }
        var file by remember { mutableStateOf<File?>(null) }
        var isPlaying by remember { mutableStateOf(false) }
        ClickButton("选择一个文件") {
            intent(selectFile("audio/*").putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("audio/*", "application/octet-stream"))).request {
                lifecycleScope.launchIO { // 不占用线程让页面返回
                    val uri = it.data?.data ?: return@launchIO
                    val doc = DocumentFile.fromSingleUri(this@AudioActivity, uri) ?: return@launchIO
                    val f = FileHelper.newFile("audio", "cache.pcm").sureExists().copyForm(doc) ?: return@launchIO
                    file = f
                    name = doc.name ?: ""
                    if (!name.endsWith(".pcm") && file != null) infoAudio(f)
                }
            }
        }
        SplitV()
        Text(name)
        SplitV()
        if (name.endsWith(".pcm") && file != null) {
            Text("pcm播放器")
            ClickButton("samplerate: ${info.sampleRate}") {
                DialogHelper.newBottom(AudioInfo.SAMPLE_RATES.map { it.toString() }.toTypedArray()) { info = info.copy(sampleRateIndex = it) }.show()
            }
            ClickButton("channel: ${info.channel.second}") {
                DialogHelper.newBottom(AudioInfo.CHANNELS.map { it.toString() }.toTypedArray()) { info = info.copy(channelIndex = it) }.show()
            }
            ClickButton("format: ${info.format.second}") {
                DialogHelper.newBottom(AudioInfo.FORMATS.map { it.toString() }.toTypedArray()) { info = info.copy(formatIndex = it) }.show()
            }
            StateButton(if (isPlaying) "STOP" else "PLAY", isPlaying) {
                if (isPlaying) {
                    currPlayer?.release()
                    isPlaying = false
                } else {
                    file?.let {
                        val player = AudioPlayer(info.sampleRate, info.channel.first, info.format.first)
                        currPlayer = player
                        lifecycleScope.launchIO {
                            isPlaying = true
                            player.prepare()
                            runCatching { player.write(it, isPlaying) }
                            isPlaying = false
                        }
                    }
                }
            }
            SplitV()

            ClickButton("转为WAV") {
                intent(selectDir()).request { }
            }
            ClickButton("转为MP3") {
                file?.let {
                    lifecycleScope.launchIO {
                        val newMp3 = newConvert(it, name, "mp3")
                        log.logStr("newMP3: $newMp3")
                    }
                }
            }
        } else {
        }
    }

    private suspend fun newConvert(from: File, name: String, newExt: String): String? {
        val result = intent(selectDir()).requestNow()
        val uri = result.data?.data ?: return null
        val doc = DocumentFile.fromTreeUri(this@AudioActivity, uri) ?: return null
        val newDoc = doc.createFile("audio/${newExt}", "${name}.${newExt}") ?: return null
        // todo convert
        contentResolver.openOutputStream(newDoc.uri)?.use { ous -> FileInputStream(from).use { ins -> ins.copyTo(ous) } }
        return newDoc.name
    }

    internal data class AudioInfo(val sampleRateIndex: Int = 2, val channelIndex: Int = 0, val formatIndex: Int = 1) {
        val sampleRate get() = SAMPLE_RATES[sampleRateIndex]
        val channel get() = CHANNELS[channelIndex]
        val format get() = FORMATS[formatIndex]

        companion object {
            internal val SAMPLE_RATES = arrayOf(8000, 11025, 16000, 22050, 44100, 48000, 96000)
            internal val CHANNELS = arrayOf(AudioFormat.CHANNEL_OUT_MONO to "CHANNEL_OUT_MONO", AudioFormat.CHANNEL_OUT_STEREO to "CHANNEL_OUT_STEREO")
            internal val FORMATS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(AudioFormat.ENCODING_PCM_8BIT to "PCM_8BIT", AudioFormat.ENCODING_PCM_16BIT to "PCM_16BIT", AudioFormat.ENCODING_PCM_32BIT to "PCM_32BIT", AudioFormat.ENCODING_PCM_FLOAT to "PCM_FLOAT")
            } else {
                arrayOf(AudioFormat.ENCODING_PCM_8BIT to "PCM_8BIT", AudioFormat.ENCODING_PCM_16BIT to "PCM_16BIT", AudioFormat.ENCODING_PCM_FLOAT to "PCM_32BIT")
            }
        }
    }

    private fun infoAudio(file: File) {
        val info = FFmpegHelper.getMediaInfo(file.absolutePath) ?: ""
        log.logStr(info)
    }
}