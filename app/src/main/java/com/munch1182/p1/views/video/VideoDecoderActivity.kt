package com.munch1182.p1.views.video

import android.content.Intent
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaFormat
import android.net.Uri
import android.os.Bundle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import com.munch1182.lib.AppHelper
import com.munch1182.lib.base.launchIO
import com.munch1182.lib.base.log
import com.munch1182.lib.helper.closeQuietly
import com.munch1182.lib.helper.result.intent
import com.munch1182.lib.helper.result.requestNow
import com.munch1182.lib.helper.sound.AudioPlayer
import com.munch1182.p1.base.BaseActivity
import com.munch1182.p1.helper.Mp42AudioConverter
import com.munch1182.p1.ui.ClickButton
import com.munch1182.p1.ui.Split
import com.munch1182.p1.ui.setContentWithRv

class VideoDecoderActivity : BaseActivity() {

    private val log = log()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentWithRv { Views() }
    }

    private fun saveUri(uri: Uri) {
        getSharedPreferences(this::class.java.canonicalName, MODE_PRIVATE).edit { putString("save_path", uri.toString()) }
    }

    private fun getUri(): Uri? {
        return getSharedPreferences(this::class.java.canonicalName, MODE_PRIVATE).getString("save_path", null)?.toUri()
    }

    private var player: AudioPlayer? = null

    @Composable
    private fun Views() {

        var lastPcmUri by remember { mutableStateOf<Uri?>(null) }
        var lastFormat by remember { mutableStateOf<MediaFormat?>(null) }
        var video by remember { mutableStateOf<DocumentFile?>(null) }
        var savePath by remember { mutableStateOf<DocumentFile?>(null) }
        val enable by remember { derivedStateOf { video != null && savePath != null } }

        savePath = getUri()?.let { DocumentFile.fromTreeUri(this, it) }
        ClickButton("测试") {
            lifecycleScope.launchIO {
                var uri = getUri()
                if (uri == null) uri = intent(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)).requestNow().data?.data
                if (uri != null) {
                    val pcm = DocumentFile.fromTreeUri(AppHelper, uri)?.listFiles()?.findLast { it.name?.endsWith(".pcm") == true }
                    log.logStr("pcm: ${pcm?.name}")
                    val uri = pcm?.uri ?: return@launchIO
                    play(uri, 16000, 1)
                }
            }
        }
        ClickButton("选择一个视频文件") {
            intent(Intent(Intent.ACTION_GET_CONTENT).setType("video/*")).request {
                video = it.data?.data?.let { it1 -> DocumentFile.fromSingleUri(this, it1) }
            }
        }
        ClickButton("选择一个保存文件夹") {
            intent(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)).request {
                val uri = it.data?.data
                savePath = uri?.let { it1 -> DocumentFile.fromTreeUri(this, it1) }
                if (uri != null) {
                    contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    saveUri(uri)
                }
            }
        }
        Split()
        if (video != null) {
            Text("${video?.name} -> ${saveName(savePath, video)}")
        }
        ClickButton("开始提取", enable = enable) {
            runCatching { player?.release() }
            player = null
            lastPcmUri = null
            try {
                val videoUri = video?.uri ?: throw IllegalStateException("videoUri is null")
                val name = video?.name?.let { "$it.pcm" } ?: "video.pcm"
                val pcmF = savePath?.createFile("audio/*", name)?.uri ?: throw IllegalStateException("pcmF is null")
                val ous = contentResolver.openOutputStream(pcmF) ?: throw IllegalStateException("ous is null")
                lifecycleScope.launchIO {
                    lastFormat = Mp42AudioConverter.convertStreaming(AppHelper, videoUri, ous)
                    log.logStr("format: $lastFormat")
                    ous.closeQuietly()
                    lastPcmUri = pcmF
                }
            } catch (e: Exception) {
                e.printStackTrace()
                lastPcmUri = null
            }
        }
        if (lastPcmUri != null && lastFormat != null) {
            val sample = runCatching { lastFormat?.getInteger(MediaFormat.KEY_SAMPLE_RATE) }.getOrNull()
            val channel = runCatching { lastFormat?.getInteger(MediaFormat.KEY_CHANNEL_COUNT) }.getOrNull()

            if (sample != null && channel != null) {

                ClickButton("播放") {
                    val nSampleRate = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC)
                    log.logStr("sample: $sample, channel: $channel, nSampleRate: $nSampleRate")
                    lifecycleScope.launchIO { play(lastPcmUri!!, nSampleRate, channel) }
                }
            }
        }
    }

    private fun saveName(path: DocumentFile?, file: DocumentFile?): String? {
        val nameD = path?.name ?: return null
        val nameF = file?.name ?: return null
        return if (nameD.contains("@")) {
            "${nameD.split("@")[1]}\n/\n$nameF.pcm"
        } else {
            "$nameD\n/\n$nameF.pcm"
        }
    }


    private fun play(uri: Uri, sampleRate: Int, channel: Int) {
        val play = AudioPlayer(sampleRate, AudioFormat.CHANNEL_IN_STEREO)
        play.prepare()
        this.player = play
        lifecycleScope.launchIO {
            contentResolver.openInputStream(uri)?.use {
                val buffer = play.newBuffer
                while (true) {
                    val read = it.read(buffer)
                    if (read <= 0) break
                    play.write(buffer, 0, read)
                }
                play.writeOver {
                    play.release()
                }
            }

        }

    }
}