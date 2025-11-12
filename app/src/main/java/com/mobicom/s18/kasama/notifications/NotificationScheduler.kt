package com.mobicom.s18.kasama.notifications

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

object NotificationScheduler {

    private const val CHORE_REMINDER_WORK = "chore_reminder_work"

    fun scheduleChoreReminders(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Schedule daily reminders at 9 AM
        val currentTime = System.currentTimeMillis()
        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 9)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
        }

        // If 9 AM has passed today, schedule for tomorrow
        if (calendar.timeInMillis <= currentTime) {
            calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
        }

        val delay = calendar.timeInMillis - currentTime

        val choreReminderRequest = PeriodicWorkRequestBuilder<ChoreReminderWorker>(
            1, TimeUnit.DAYS
        )
            .setConstraints(constraints)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag(CHORE_REMINDER_WORK)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            CHORE_REMINDER_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            choreReminderRequest
        )
    }

    fun cancelChoreReminders(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(CHORE_REMINDER_WORK)
    }

    fun scheduleImmediateChoreCheck(context: Context) {
        val immediateWorkRequest = OneTimeWorkRequestBuilder<ChoreReminderWorker>()
            .build()

        WorkManager.getInstance(context).enqueue(immediateWorkRequest)
    }
}