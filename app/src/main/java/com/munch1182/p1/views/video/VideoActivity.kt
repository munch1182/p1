package com.munch1182.p1.views.video

import android.os.Bundle
import androidx.compose.runtime.Composable
import com.munch1182.p1.base.BaseActivity
import com.munch1182.p1.ui.JumpButton
import com.munch1182.p1.ui.setContentWithRv

class VideoActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentWithRv { Views() }
    }

    @Composable
    private fun Views() {
        JumpButton("视频提取", clazz = VideoSpiderActivity::class)
        JumpButton("音频提取", clazz = VideoDecoderActivity::class)
    }
}