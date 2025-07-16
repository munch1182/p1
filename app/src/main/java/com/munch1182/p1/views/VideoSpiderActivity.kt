package com.munch1182.p1.views

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.munch1182.lib.base.launchIO
import com.munch1182.lib.helper.ClipboardHelper
import com.munch1182.p1.base.BaseActivity
import com.munch1182.p1.base.bind
import com.munch1182.p1.databinding.ActivityVideoSpiderBinding
import com.munch1182.p1.helper.NetVideoHelper

class VideoSpiderActivity : BaseActivity() {

    private val bind by bind(ActivityVideoSpiderBinding::inflate)

    @SuppressLint("SetTextI18n", "SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind.web.settings.javaScriptEnabled = true
        bind.confirm.setOnClickListener {
            val input = bind.edit.text.toString()
            if (input.isEmpty()) return@setOnClickListener
            val shareUrl = NetVideoHelper.getFirstUrl(input)
            bind.res.text = "url: $shareUrl"
            shareUrl ?: return@setOnClickListener
            lifecycleScope.launchIO {
                val url = NetVideoHelper.parseVideoUrl(shareUrl, bind.web)
                bind.res.post { bind.res.text = "url: $shareUrl\nres: $url" }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        bind.edit.postDelayed({
            val str = ClipboardHelper.copyFrom2Str()
            bind.edit.setText(str)
        }, 300L)
    }
}