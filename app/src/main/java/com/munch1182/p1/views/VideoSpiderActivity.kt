package com.munch1182.p1.views

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import com.munch1182.lib.base.launchIO
import com.munch1182.lib.helper.ClipboardHelper
import com.munch1182.lib.helper.result.intent
import com.munch1182.p1.base.BaseActivity
import com.munch1182.p1.base.bind
import com.munch1182.p1.base.toast
import com.munch1182.p1.databinding.ActivityVideoSpiderBinding
import com.munch1182.p1.helper.NetVideoHelper
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
            bind.openContainer.visibility = android.view.View.GONE
            val input = bind.edit.text.toString()
            if (input.isEmpty()) return@setOnClickListener
            val shareUrl = NetVideoHelper.getFirstUrl(input)
            bind.res.text = "url: $shareUrl"
            shareUrl ?: return@setOnClickListener
            lifecycleScope.launchIO {
                val url = NetVideoHelper.parseVideoUrl(shareUrl, bind.web)
                bind.res.post {
                    bind.res.text = "url: $shareUrl\nres: $url"
                    if (!url.isNullOrEmpty()) {
                        bind.openContainer.visibility = android.view.View.VISIBLE
                    } else {
                        bind.openContainer.visibility = android.view.View.GONE
                    }
                    bind.download.setOnClickListener { download(url) }
                    bind.play.setOnClickListener { play(url) }
                }
            }
        }
    }

    private fun download(url: String?) {
        url ?: return
        intent(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)).request {
            val uri = it.data?.data
            val file = file(uri)
            if (file == null) {
                toast("选择文件夹失败")
                return@request
            }

            lifecycleScope.launchIO { downByte(url, file) }
        }
    }

    private fun downByte(url: String, file: DocumentFile) {
        val req = Request.Builder().url(url).get().build()
        val response = client.newCall(req).execute()

        if (!response.isSuccessful) {
            return
        }

        val out = contentResolver.openOutputStream(file.uri) ?: return
        val ins = response.body.byteStream()
        ins.copyTo(out)
        ins.closeQuietly()
        out.closeQuietly()
    }

    private fun file(uri: Uri? = null): DocumentFile? {
        uri ?: return null
        //contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        val dir = DocumentFile.fromTreeUri(this, uri) ?: return null
        val file = dir.createFile("video/*", "video.mp4")
        return file
    }

    private fun play(url: String?) {

    }

    override fun onResume() {
        super.onResume()
        bind.edit.postDelayed({
            val str = ClipboardHelper.copyFrom2Str()
            bind.edit.setText(str)
        }, 300L)
    }
}