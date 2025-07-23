package com.munch1182.p1.views

import android.annotation.SuppressLint
import android.content.Intent
import android.media.AudioFormat
import android.media.MediaFormat
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import com.munch1182.lib.base.launchIO
import com.munch1182.lib.helper.ClipboardHelper
import com.munch1182.lib.helper.result.intent
import com.munch1182.lib.helper.result.requestNow
import com.munch1182.lib.helper.sound.wavHeader
import com.munch1182.p1.base.BaseActivity
import com.munch1182.p1.base.bind
import com.munch1182.p1.base.toast
import com.munch1182.p1.databinding.ActivityVideoSpiderBinding
import com.munch1182.p1.helper.Mp42AudioConverter
import com.munch1182.p1.helper.NetVideoHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.internal.closeQuietly
import okhttp3.logging.HttpLoggingInterceptor

class VideoSpiderActivity : BaseActivity() {

    private val bind by bind(ActivityVideoSpiderBinding::inflate)
    private val client by lazy {
        OkHttpClient.Builder().addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        }).build()
    }

    @SuppressLint("SetTextI18n", "SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind.web.settings.javaScriptEnabled = true
        bind.confirm.setOnClickListener {
            resetView()
            val input = bind.edit.text.toString()
            if (input.isEmpty()) return@setOnClickListener
            val shareUrl = NetVideoHelper.getFirstUrl(input)
            bind.res.text = "url: $shareUrl"
            shareUrl ?: return@setOnClickListener
            lifecycleScope.launchIO {
                val result = NetVideoHelper.parseVideoUrl(shareUrl, bind.web)
                bind.res.post {
                    setupResult(result, shareUrl)
                    setDownClick(result)
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupResult(result: NetVideoHelper.ParseResult?, shareUrl: String) {
        bind.res.text = "url: $shareUrl\nres: ${result?.url}"
        if (result?.isSuccess == true) {
            bind.openContainer.visibility = View.VISIBLE
        } else {
            bind.openContainer.visibility = View.GONE
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setDownClick(result: NetVideoHelper.ParseResult?) {
        if (result?.isSuccess != true) return
        bind.download.setOnClickListener {
            lifecycleScope.launchIO {
                val uri = download(result)
                val isSuccess = uri != null
                withContext(Dispatchers.Main) {
                    bind.res.text = "${bind.res.text}\n${if (isSuccess) "下载成功" else "下载失败"}"
                    if (isSuccess) {
                        bind.convert.visibility = View.VISIBLE
                        setConvertClick(uri, result)
                    }
                }
            }
        }
    }

    private fun setConvertClick(uri: Uri?, result: NetVideoHelper.ParseResult?) {
        uri ?: return
        bind.convert.setOnClickListener {
            lifecycleScope.launchIO {
                try {

                    val dirUri = loadUri() ?: return@launchIO
                    val dir = DocumentFile.fromTreeUri(this@VideoSpiderActivity, dirUri) ?: return@launchIO
                    val file = dir.createFile("video/*", result?.title?.let { "${it}.pcm" } ?: "video.pcm") ?: return@launchIO

                    val ous = contentResolver.openOutputStream(file.uri) ?: return@launchIO
                    val format = Mp42AudioConverter.convertStreaming(this@VideoSpiderActivity, uri, ous) ?: return@launchIO
                    ous.closeQuietly()

                    val length = file.length()

                    try {
                        val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        val channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        val encoding = try {
                            format.getInteger(MediaFormat.KEY_PCM_ENCODING)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            AudioFormat.ENCODING_PCM_16BIT
                        }
                        val header = wavHeader(sampleRate, channelCount, encoding, length)

                        val wavFile = dir.createFile("video/*", result?.title?.let { "${it}.wav" } ?: "video.wav") ?: return@launchIO
                        val ousWav = contentResolver.openOutputStream(wavFile.uri) ?: return@launchIO
                        ousWav.write(header)
                        val ins = contentResolver.openInputStream(file.uri) ?: return@launchIO
                        ins.copyTo(ousWav)
                        ousWav.closeQuietly()
                        ins.closeQuietly()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun resetView() {
        bind.openContainer.visibility = View.GONE
        bind.convert.visibility = View.GONE
    }

    companion object {
        private const val KEY_TREE_URI = "ACTION_OPEN_DOCUMENT_TREE"
    }

    private fun loadUri(): Uri? {
        return getSharedPreferences(this::class.java.canonicalName, MODE_PRIVATE).getString(KEY_TREE_URI, null)?.toUri()
    }

    private fun saveUri(uri: Uri) {
        getSharedPreferences(this::class.java.canonicalName, MODE_PRIVATE).edit { putString(KEY_TREE_URI, uri.toString()) }
    }

    private suspend fun download(result: NetVideoHelper.ParseResult?): Uri? {
        var uri = loadUri()
        if (uri != null) {
            return file(uri, result)?.let { downByte(result, it) }
        }
        val data = intent(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)).requestNow()
        uri = data.data?.data
        val file = file(uri, result)
        if (file == null) {
            toast("选择文件夹失败")
            return null
        }
        uri ?: return null
        saveUri(uri)
        contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        delay(300L)
        return downByte(result, file)
    }

    private fun downByte(result: NetVideoHelper.ParseResult?, file: DocumentFile): Uri? {
        val url = result?.url ?: return null
        val req = Request.Builder().url(url).get().build()
        val response = client.newCall(req).execute()

        if (!response.isSuccessful) {
            return null
        }

        val out = contentResolver.openOutputStream(file.uri) ?: return null
        val ins = response.body.byteStream()
        ins.copyTo(out)
        ins.closeQuietly()
        out.closeQuietly()
        return file.uri
    }

    private fun file(uri: Uri? = null, result: NetVideoHelper.ParseResult?): DocumentFile? {
        uri ?: return null
        val dir = DocumentFile.fromTreeUri(this, uri) ?: return null
        val file = dir.createFile("video/*", result?.title?.let { "${it}.mp4" } ?: "video.mp4")
        return file
    }

    override fun onResume() {
        super.onResume()
        bind.edit.postDelayed({
            val str = ClipboardHelper.copyFrom2Str()
            bind.edit.setText(str)
        }, 300L)
    }
}