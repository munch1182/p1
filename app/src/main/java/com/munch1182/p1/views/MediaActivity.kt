package com.munch1182.p1.views

import android.content.ContentResolver
import android.content.Intent
import android.media.browse.MediaBrowser
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.service.media.MediaBrowserService
import android.view.KeyEvent
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.RawResourceDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.munch1182.lib.base.log
import com.munch1182.lib.base.onDestroyed
import com.munch1182.lib.helper.currAct
import com.munch1182.lib.helper.currAsFM
import com.munch1182.p1.R
import com.munch1182.p1.base.BaseActivity
import com.munch1182.p1.ui.ClickButton
import com.munch1182.p1.ui.setContentWithRv

// https://developer.android.google.cn/media/media3?hl=zh-cn
class MediaActivity : BaseActivity() {

    private val log = log()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentWithRv { Views() }
    }

    private val callback = object : MediaBrowser.ConnectionCallback() {
        override fun onConnected() {
            super.onConnected()
            log.logStr("onConnected")
        }

        override fun onConnectionFailed() {
            super.onConnectionFailed()
            log.logStr("onConnectionFailed")
        }

        override fun onConnectionSuspended() {
            super.onConnectionSuspended()
            log.logStr("onConnectionSuspended")
        }
    }

    @OptIn(UnstableApi::class)
    @Composable
    private fun Views() {
        ClickButton("开始监听") {
            val play = ExoPlayer.Builder(currAct).build()
            val uri = Uri.Builder().scheme(ContentResolver.SCHEME_ANDROID_RESOURCE).path(R.raw.silence10sec.toString()).build()
            val uriWrapper = RawResourceDataSource(currAct).apply { open(DataSpec(uri)) }.uri!!
            play.setMediaItem(MediaItem.fromUri(uriWrapper))
            play.repeatMode = Player.REPEAT_MODE_ALL
            play.prepare()
            play.play()
            currAsFM.lifecycle.onDestroyed { play.release() }
            log.logStr("connect")
            MediaSession.Builder(currAct, play).setCallback(object : MediaSession.Callback {
                @UnstableApi
                override fun onMediaButtonEvent(session: MediaSession, controllerInfo: MediaSession.ControllerInfo, intent: Intent): Boolean {
                    val key = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
                    } else {
                        @Suppress("DEPRECATION") intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
                    }
                    log.logStr("onMediaButtonEvent: ${key?.let { "${it.keyCode}, ${it.action}, ${it.scanCode}" }}")
                    return true
                }
            }).build()
        }
    }

}

/**
 * <service
 *    android:name=".views.MediaService"
 *    android:exported="false">
 *    <intent-filter>
 *      <action android:name="android.media.browse.MediaBrowserService" />
 *    </intent-filter>
 * </service>
 */
class MediaService : MediaBrowserService() {
    private val log = log()

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot {
        log.logStr("onGetRoot")
        return BrowserRoot("ROOTID", null)
    }

    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowser.MediaItem>>) {
    }
}