package com.princesanitary.grahak.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.princesanitary.grahak.MainActivity
import com.princesanitary.grahak.R

/**
 * Helper to display high-priority system notifications for orders and bookings
 * with authentic iPhone chime notification sound and quick interactive actions.
 */
object NotificationHelper {
    private const val ORDER_CHANNEL_ID = "fixit_owner_orders_v2"
    private const val PLUMBER_CHANNEL_ID = "fixit_plumber_jobs_v2"

    const val ACTION_ACCEPT_ORDER = "com.princesanitary.grahak.ACTION_ACCEPT_ORDER"
    const val ACTION_VIEW_ORDER = "com.princesanitary.grahak.ACTION_VIEW_ORDER"
    const val ACTION_ACCEPT_JOB = "com.princesanitary.grahak.ACTION_ACCEPT_JOB"
    const val ACTION_VIEW_JOB = "com.princesanitary.grahak.ACTION_VIEW_JOB"

    const val EXTRA_ORDER_ID = "extra_order_id"
    const val EXTRA_JOB_ID = "extra_job_id"

    fun showOrderNotification(
        context: Context,
        title: String = "New order received",
        body: String,
        orderId: String? = null,
        notificationId: Int = (System.currentTimeMillis() % 100000).toInt()
    ) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val soundUri = Uri.parse(
            ContentResolver.SCHEME_ANDROID_RESOURCE + "://" +
                context.packageName + "/" + R.raw.iphone_notification
        )

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ORDER_CHANNEL_ID,
                "New Store Orders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Real-time alerts with iPhone chime for incoming customer orders"
                enableVibration(true)
                setSound(soundUri, audioAttributes)
            }
            manager.createNotificationChannel(channel)
        }

        // Play crisp iPhone chime directly in-app
        try {
            MediaPlayer.create(context, R.raw.iphone_notification)?.apply {
                setOnCompletionListener { mp ->
                    mp.stop()
                    mp.release()
                }
                start()
            }
        } catch (_: Exception) {}

        val viewIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_VIEW_ORDER
            if (orderId != null) putExtra(EXTRA_ORDER_ID, orderId)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val viewPendingIntent = PendingIntent.getActivity(
            context,
            notificationId * 2,
            viewIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val acceptIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_ACCEPT_ORDER
            if (orderId != null) putExtra(EXTRA_ORDER_ID, orderId)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val acceptPendingIntent = PendingIntent.getActivity(
            context,
            notificationId * 2 + 1,
            acceptIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, ORDER_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(if (title.isBlank()) "New order received" else title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(soundUri)
            .setContentIntent(viewPendingIntent)
            .addAction(R.mipmap.ic_launcher, "Accept", acceptPendingIntent)
            .addAction(R.mipmap.ic_launcher, "View Details", viewPendingIntent)

        manager.notify(notificationId, builder.build())
    }

    fun showPlumberJobNotification(
        context: Context,
        title: String = "New job request available",
        body: String,
        jobId: String? = null,
        notificationId: Int = (System.currentTimeMillis() % 100000).toInt()
    ) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val soundUri = Uri.parse(
            ContentResolver.SCHEME_ANDROID_RESOURCE + "://" +
                context.packageName + "/" + R.raw.iphone_notification
        )

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                PLUMBER_CHANNEL_ID,
                "New Job Requests",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts with iPhone chime for available plumbing jobs"
                enableVibration(true)
                setSound(soundUri, audioAttributes)
            }
            manager.createNotificationChannel(channel)
        }

        // Play chime in-app
        try {
            MediaPlayer.create(context, R.raw.iphone_notification)?.apply {
                setOnCompletionListener { mp ->
                    mp.stop()
                    mp.release()
                }
                start()
            }
        } catch (_: Exception) {}

        val viewIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_VIEW_JOB
            if (jobId != null) putExtra(EXTRA_JOB_ID, jobId)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val viewPendingIntent = PendingIntent.getActivity(
            context,
            notificationId * 2,
            viewIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val acceptIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_ACCEPT_JOB
            if (jobId != null) putExtra(EXTRA_JOB_ID, jobId)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val acceptPendingIntent = PendingIntent.getActivity(
            context,
            notificationId * 2 + 1,
            acceptIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, PLUMBER_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(if (title.isBlank()) "New job request available" else title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(soundUri)
            .setContentIntent(viewPendingIntent)
            .addAction(R.mipmap.ic_launcher, "Accept Job", acceptPendingIntent)
            .addAction(R.mipmap.ic_launcher, "View Request", viewPendingIntent)

        manager.notify(notificationId, builder.build())
    }

    fun showNotification(
        context: Context,
        title: String,
        body: String,
        notificationId: Int = (System.currentTimeMillis() % 100000).toInt()
    ) {
        showOrderNotification(context, title, body, null, notificationId)
    }
}
