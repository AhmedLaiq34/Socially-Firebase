package com.example.i230657

import android.app.PendingIntent
import android.content.Intent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val type = remoteMessage.data["type"] ?: ""
        val title = remoteMessage.notification?.title ?: "Notification"
        val body = remoteMessage.notification?.body ?: "You have a new notification"

        val channelId = when (type) {
            "follow_request" -> "follow_requests"
            "screenshot" -> "screenshots"
            else -> "messages"
        }

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = when (type) {
                "follow_request" -> "Follow Requests"
                "screenshot" -> "Screenshots"
                else -> "Messages"
            }
            val importance = if (type == "screenshot") {
                NotificationManager.IMPORTANCE_HIGH
            } else {
                NotificationManager.IMPORTANCE_HIGH
            }
            val channel = NotificationChannel(channelId, channelName, importance)
            manager.createNotificationChannel(channel)
        }

        val intent = when (type) {
            "follow_request" -> Intent(this, notifications_page::class.java)
            "message" -> Intent(this, chat_page::class.java).apply {
                putExtra("receiverId", remoteMessage.data["receiverId"])
            }
            "screenshot" -> Intent(this, chat_page::class.java).apply {
                putExtra("receiverId", remoteMessage.data["senderId"])
            }
            else -> null
        }

        val pendingIntent = intent?.let {
            PendingIntent.getActivity(
                this,
                0,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}