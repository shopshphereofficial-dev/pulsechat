package com.pulsechat.app.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

object Notify {
    private const val CH = "pulsechat_messages"

    fun ensure(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        val ch = NotificationChannel(CH, "Messages", NotificationManager.IMPORTANCE_DEFAULT)
        nm.createNotificationChannel(ch)
    }

    fun message(context: Context, title: String, text: String, id: Int) {
        try {
            ensure(context)
            val n = NotificationCompat.Builder(context, CH)
                .setSmallIcon(android.R.drawable.ic_dialog_email)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .build()
            context.getSystemService(NotificationManager::class.java).notify(id, n)
        } catch (_: Exception) {
        }
    }
}
