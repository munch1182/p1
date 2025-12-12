package com.munch1182.android.lib.helper

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.munch1182.android.lib.AppHelper
import java.util.concurrent.atomic.AtomicInteger

object NoticeHelper {

    val nmCompat by lazy { NotificationManagerCompat.from(AppHelper) }

    val isEnable get() = nmCompat.areNotificationsEnabled()

    private val ids by lazy { AtomicInteger() }

    val newId get() = ids.getAndIncrement()

    /**
     * 检查[channelId]是否存在，不存在则创建
     */
    fun checkOrCreateChannel(channelId: String, channelName: String, desc: String? = null, importance: Int = NotificationManager.IMPORTANCE_DEFAULT): NotificationChannel? {
        return nmCompat.getNotificationChannel(channelId) ?: createChannel(channelId, channelName, desc, importance)
    }

    /**
     * 检查[channelId]是否存在，并且是否被禁用
     */
    fun checkChannelIsDisable(channelId: String) = nmCompat.getNotificationChannel(channelId)?.let { it.importance == NotificationManagerCompat.IMPORTANCE_NONE } ?: false

    /**
     * 创建通知渠道
     */
    fun createChannel(channelId: String, channelName: String, desc: String? = null, importance: Int = NotificationManager.IMPORTANCE_DEFAULT): NotificationChannel? {
        val channel = NotificationChannel(channelId, channelName, importance)
        channel.description = desc
        runCatching { nmCompat.createNotificationChannel(channel) }.getOrNull() ?: return null
        return channel
    }

    /**
     * 发送通知
     *
     * @param notifyId 通知id，如果有相同的id存在，则会更新该Notification
     */
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun send(notifyBuilder: NotificationCompat.Builder, notifyId: Int = newId, tag: String? = null): Int {
        nmCompat.notify(tag, notifyId, notifyBuilder.build())
        return notifyId
    }

    fun cancel(notifyId: Int, tag: String? = null) {
        nmCompat.cancel(tag, notifyId)
    }

    fun cancelAll() {
        nmCompat.cancelAll()
    }
}