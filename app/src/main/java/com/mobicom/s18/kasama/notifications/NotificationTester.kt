package com.mobicom.s18.kasama.notifications

import android.content.Context
import java.util.*

object NotificationTester {

    fun testChoreReminder(context: Context) {
        NotificationHelper.showChoreReminderNotification(
            context = context,
            choreId = "test_chore_${System.currentTimeMillis()}",
            choreTitle = "Test Chore: Clean Kitchen",
            dueDate = "Today",
            isOverdue = false
        )
    }

    fun testOverdueChore(context: Context) {
        NotificationHelper.showChoreReminderNotification(
            context = context,
            choreId = "test_overdue_${System.currentTimeMillis()}",
            choreTitle = "Test Overdue: Vacuum Living Room",
            dueDate = "Yesterday",
            isOverdue = true
        )
    }

    fun testHouseholdNotification(context: Context) {
        NotificationHelper.showHouseholdNotification(
            context = context,
            title = "Test: New Member",
            message = "John Doe joined your household"
        )
    }

    fun testGeneralNotification(context: Context) {
        NotificationHelper.showGeneralNotification(
            context = context,
            title = "Test: General Info",
            message = "This is a test general notification"
        )
    }
}