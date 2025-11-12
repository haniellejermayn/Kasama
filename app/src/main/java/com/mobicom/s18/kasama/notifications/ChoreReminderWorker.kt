package com.mobicom.s18.kasama.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mobicom.s18.kasama.KasamaApplication
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

class ChoreReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val app = applicationContext as KasamaApplication
            val currentUser = app.firebaseAuth.currentUser

            if (currentUser == null) {
                return Result.success()
            }

            val userResult = app.userRepository.getUserById(currentUser.uid)
            val householdId = userResult.getOrNull()?.householdId

            if (householdId == null) {
                return Result.success()
            }

            // Get all incomplete chores for the user
            val chores = app.choreRepository.getChoresByHousehold(householdId).first()
                .filter { it.assignedTo == currentUser.uid && !it.isCompleted }

            val calendar = Calendar.getInstance()
            val today = calendar.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val tomorrow = calendar.apply {
                add(Calendar.DAY_OF_MONTH, 1)
            }.timeInMillis

            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH)

            chores.forEach { chore ->
                val dueDate = chore.dueDate

                when {
                    // Overdue chores
                    dueDate < today -> {
                        NotificationHelper.showChoreReminderNotification(
                            context = applicationContext,
                            choreId = chore.id,
                            choreTitle = chore.title,
                            dueDate = dateFormat.format(Date(dueDate)),
                            isOverdue = true
                        )
                    }
                    // Due today
                    dueDate >= today && dueDate < tomorrow -> {
                        NotificationHelper.showChoreReminderNotification(
                            context = applicationContext,
                            choreId = chore.id,
                            choreTitle = chore.title,
                            dueDate = "today",
                            isOverdue = false
                        )
                    }
                    // Due tomorrow
                    dueDate >= tomorrow && dueDate < tomorrow + 86400000 -> {
                        NotificationHelper.showChoreReminderNotification(
                            context = applicationContext,
                            choreId = chore.id,
                            choreTitle = chore.title,
                            dueDate = "tomorrow",
                            isOverdue = false
                        )
                    }
                }
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}