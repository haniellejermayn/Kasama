package com.mobicom.s18.kasama.data.local

import android.content.Context
import android.content.SharedPreferences

class NotificationPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "kasama_notification_prefs",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_CHORE_REMINDERS_ENABLED = "chore_reminders_enabled"
        private const val KEY_HOUSEHOLD_UPDATES_ENABLED = "household_updates_enabled"
        private const val KEY_REMINDER_TIME_HOUR = "reminder_time_hour"
        private const val KEY_REMINDER_TIME_MINUTE = "reminder_time_minute"
    }

    var choreRemindersEnabled: Boolean
        get() = prefs.getBoolean(KEY_CHORE_REMINDERS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_CHORE_REMINDERS_ENABLED, value).apply()

    var householdUpdatesEnabled: Boolean
        get() = prefs.getBoolean(KEY_HOUSEHOLD_UPDATES_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_HOUSEHOLD_UPDATES_ENABLED, value).apply()

    var reminderTimeHour: Int
        get() = prefs.getInt(KEY_REMINDER_TIME_HOUR, 9)
        set(value) = prefs.edit().putInt(KEY_REMINDER_TIME_HOUR, value).apply()

    var reminderTimeMinute: Int
        get() = prefs.getInt(KEY_REMINDER_TIME_MINUTE, 0)
        set(value) = prefs.edit().putInt(KEY_REMINDER_TIME_MINUTE, value).apply()
}