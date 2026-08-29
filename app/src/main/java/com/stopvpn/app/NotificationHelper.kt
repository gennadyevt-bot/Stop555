package com.stopvpn.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationHelper {
    const val CHANNEL_ID = "stopvpn_channel"
    const val NOTIFICATION_ID = 1

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "STOP VPN",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Уведомление активного VPN-соединения"
                setShowBadge(false)
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    fun buildNotification(context: Context, serverName: String): android.app.Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("STOP VPN активен")
            .setContentText("Сервер: $serverName")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
}
