package com.princesanitary.grahak.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.princesanitary.grahak.MainActivity
import com.princesanitary.grahak.R
import com.princesanitary.grahak.di.ServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Receives pushes and keeps this device's FCM token on the user's profile.
 *
 * Both methods here were previously empty (`// Send token to server`), so no
 * token was ever stored and nothing could ever be pushed to a device. A plumber
 * could only find out about a job by having the app open on the Jobs tab.
 */
class GrahakFirebaseMessagingService : FirebaseMessagingService() {

    // The service can be torn down right after a callback, so work is launched
    // on an application-lifetime scope rather than tied to the service.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title
            ?: message.data["title"]
            ?: "FixIt"
        val body = message.notification?.body
            ?: message.data["body"]
            ?: return

        showNotification(title, body)
    }

    /**
     * Fired when FCM issues or rotates the token. Persisting it here is what
     * makes the token survive reinstalls and app-data clears.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        scope.launch {
            // No-op when signed out; the token is re-synced after the next login.
            ServiceLocator.userRepository.saveDeviceToken(token)
        }
    }

    private fun showNotification(title: String, body: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Job & order updates",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "New job requests, order status and payouts"
                }
            )
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private companion object {
        const val CHANNEL_ID = "fixit_updates"
    }
}
