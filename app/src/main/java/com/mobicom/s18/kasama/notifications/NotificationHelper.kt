package com.mobicom.s18.kasama.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.mobicom.s18.kasama.DashboardActivity
import com.mobicom.s18.kasama.R

object NotificationHelper {
    private const val CHANNEL_ID_CHORES = "chores_channel"
    private const val CHANNEL_ID_HOUSEHOLD = "household_channel"
    private const val CHANNEL_ID_GENERAL = "general_channel"

    private const val CHANNEL_NAME_CHORES = "Chore Reminders"
    private const val CHANNEL_NAME_HOUSEHOLD = "Household Updates"
    private const val CHANNEL_NAME_GENERAL = "General Notifications"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // chores channel - high prio
            val choresChannel = NotificationChannel(
                CHANNEL_ID_CHORES,
                CHANNEL_NAME_CHORES,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for upcoming and overdue chores"
                enableVibration(true)
                enableLights(true)
            }

            // household channel - default prio
            val householdChannel = NotificationChannel(
                CHANNEL_ID_HOUSEHOLD,
                CHANNEL_NAME_HOUSEHOLD,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for household activities"
            }

            // general channel - low prio
            val generalChannel = NotificationChannel(
                CHANNEL_ID_GENERAL,
                CHANNEL_NAME_GENERAL,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "General app notifications"
            }

            notificationManager.createNotificationChannel(choresChannel)
            notificationManager.createNotificationChannel(householdChannel)
            notificationManager.createNotificationChannel(generalChannel)
        }
    }

    fun showChoreReminderNotification(
        context: Context,
        choreId: String,
        choreTitle: String,
        dueDate: String,
        isOverdue: Boolean = false
    ) {
        val intent = Intent(context, DashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("chore_id", choreId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            choreId.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = if (isOverdue) "Overdue Chore!" else "Chore Reminder"
        val message = if (isOverdue) {
            "$choreTitle is overdue!"
        } else {
            "$choreTitle is due on $dueDate"
        }

        // TODO: currently using kasama_profile_default but can update to actual notif icon(s)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_CHORES)
            .setSmallIcon(R.drawable.kasama_profile_default)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            NotificationManagerCompat.from(context).notify(choreId.hashCode(), notification)
        }
    }

    fun showHouseholdNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int = (System.currentTimeMillis() / 1000).toInt()
    ) {
        val intent = Intent(context, DashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_HOUSEHOLD)
            .setSmallIcon(R.drawable.kasama_profile_default)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }
    }

    fun showGeneralNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int = (System.currentTimeMillis() / 1000).toInt()
    ) {
        val intent = Intent(context, DashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_GENERAL)
            .setSmallIcon(R.drawable.kasama_profile_default)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }
    }

    fun cancelNotification(context: Context, notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }
}