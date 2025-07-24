package com.munch1182.p1.views.video

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import com.munch1182.lib.base.launchIO
import com.munch1182.lib.base.log
import com.munch1182.lib.helper.ClipboardHelper
import com.munch1182.lib.helper.result.intent
import com.munch1182.lib.helper.result.requestNow
import com.munch1182.p1.base.BaseActivity
import com.munch1182.p1.base.bind
import com.munch1182.p1.base.toast
import com.munch1182.p1.databinding.ActivityVideoSpiderBinding
import com.munch1182.p1.helper.NetVideoHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.internal.closeQuietly
import okhttp3.logging.HttpLoggingInterceptor

class VideoSpiderActivity : BaseActivity() {

    private val log = log()
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
        bind.res.text = "url: $shareUrl\nres: ${result?.url?.let { if (it.length > 30) "${it.substring(0, 30)}..." else it }}"
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
                }
            }
        }
    }

    private fun resetView() {
        bind.openContainer.visibility = View.GONE
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
            log.logStr("down by uri: $uri")
            return file(uri, result)?.let { downByte(result, it) }
        }
        log.logStr("down by chose")
        val data = intent(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)).requestNow()
        uri = data.data?.data
        val file = file(uri, result)
        if (file == null) {
            log.logStr("chose fail")
            toast("选择文件夹失败")
            return null
        }
        uri ?: return null
        saveUri(uri)
        contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        delay(300L)
        log.logStr("chose success")
        return downByte(result, file)
    }

    private fun downByte(result: NetVideoHelper.ParseResult?, file: DocumentFile): Uri? {
        val url = result?.url ?: return null
        val req = Request.Builder().url(url).get().build()
        val response = client.newCall(req).execute()

        log.logStr("response: ${response.code}: ${response.isSuccessful}")
        if (!response.isSuccessful) {
            return null
        }

        val out = contentResolver.openOutputStream(file.uri) ?: return null
        val ins = response.body.byteStream()
        ins.copyTo(out)
        ins.closeQuietly()
        out.closeQuietly()
        log.logStr("download complete")
        return file.uri
    }

    private fun file(uri: Uri? = null, result: NetVideoHelper.ParseResult?): DocumentFile? {
        uri ?: return null
        val dir = DocumentFile.fromTreeUri(this, uri) ?: return null
        val name = result?.title?.let { if (it.length > 7) it.substring(0, 7) else it }
        val file = dir.createFile("video/*", name?.let { "$it.mp4" } ?: "video.mp4")
        log.logStr("create $name: ${file != null}")
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