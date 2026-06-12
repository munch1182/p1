package com.munch1182.lib.android

import android.Manifest
import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * 兼容判断是否有通知权限(有权限且允许通知)
 *
 * 需要说明的是:
 * 当应用在前台时, 即使没有通知权限, 仍可以成功创建前台服务(且不会显示通知);
 * 但应用在后台时, 无权限启动前台服务则会直接崩溃;
 */
fun hasNotificationPermission(): Boolean {
    if (!isNotificationEnable) return false
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.POST_NOTIFICATIONS.isPermissionGranted()
    } else {
        true
    }
}

@RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
fun notify(notification: Notification, notificationId: Int) {
    NotificationManagerCompat.from(AppHelper).notify(notificationId, notification)
}

/**
 * 通知开关是否打开
 */
val isNotificationEnable get() = NotificationManagerCompat.from(AppHelper).areNotificationsEnabled()

/**
 * 兼容创建一个NotificationChannel
 */
fun createNotificationChannel(
    channelId: String, //
    name: String, //
    description: String, //
    importance: Int = NotificationManagerCompat.IMPORTANCE_HIGH
) {
    val channel = NotificationChannelCompat.Builder(channelId, importance) //
        .setName(name) //
        .setDescription(description) //
        .build() //
    NotificationManagerCompat.from(AppHelper).createNotificationChannel(channel)
}

/**
 * 创建一个通知对象
 */
fun createNotification(
    title: String, msg: String, smallIcon: Int,
    channelId: String,
    ongoing: Boolean = false,
    ctx: Context = AppHelper,
    set: NotificationCompat.Builder.() -> Unit = {}
) = NotificationCompat.Builder(ctx, channelId) //
    .setContentTitle(title) //
    .setContentText(msg) //
    .setSmallIcon(smallIcon) //
    .setOngoing(ongoing) //
    .apply(set) //
    .build() //

/**
 * 将本服务提升为前台服务, 在[Service]中调用
 *
 * @param foregroundServiceType 服务类型, 要与服务注册中填写的一致
 */
fun Service.startForegroundCompat(
    notificationId: Int, notification: Notification, foregroundServiceType: Int
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        startForeground(notificationId, notification, foregroundServiceType)
    } else {
        startForeground(notificationId, notification)
    }
}

/**
 * 开启通知的intent
 *
 * 权限: Manifest.permission.POST_NOTIFICATIONS
 */
val enableNotificationIntent
    get() = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, AppHelper.packageName)
    }

