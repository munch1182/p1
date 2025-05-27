package com.munch1182.lib.helper.sound

import android.app.Notification
import android.media.MediaMetadata
import android.media.session.MediaSessionManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.munch1182.lib.base.ctx
import com.munch1182.lib.base.log
import com.munch1182.lib.helper.ARDefaultManager
import com.munch1182.lib.helper.ARManager


object MediaHelper {
    private val log = log()
    val sessionManager by lazy { ctx.getSystemService(MediaSessionManager::class.java) }

    fun registerActivity() {
        sessionManager.getActiveSessions(null)?.let {
            log.logStr("$it")
        }
    }
}

/**
 * 注册：
 * <service android:name=".MusicNotificationListener"
 *     android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"
 *     android:exported="true">
 *     <intent-filter>
 *         <action android:name="android.service.notification.NotificationListenerService" />
 *     </intent-filter>
 * </service>
 *
 * 权限判断：
 *  judge { NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(packageName) }
 *      .intent(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
 *      .request {}
 */
class MusicNotificationListener : NotificationListenerService(), ARManager<MusicNotificationListener.OnMusicUpdate> by ARDefaultManager() {

    private val log = log()

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (isMediaNotification(sbn)) {
            val extras = sbn.notification.extras
            val title = extras.getString(Notification.EXTRA_TITLE) ?: extras.getString(MediaMetadata.METADATA_KEY_TITLE)
            val artist = extras.getString(Notification.EXTRA_TEXT) ?: extras.getString(MediaMetadata.METADATA_KEY_ARTIST)
            log.logStr("onNotificationPosted: $title($artist)")
            forEach { it.onUpdate(title, artist) }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
        if (isMediaNotification(sbn)) {
            log.logStr("onNotificationRemoved")
            forEach { it.onUpdate(null, null) }
        }
    }

    private fun isMediaNotification(sbn: StatusBarNotification): Boolean {
        // 通过分类判断是否为媒体通知
        return Notification.CATEGORY_TRANSPORT == sbn.notification.category
    }

    fun interface OnMusicUpdate {
        fun onUpdate(title: String?, artist: String?)
    }
}