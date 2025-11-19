package com.mobicom.s18.kasama.utils

import java.util.*

object RecurringChoreHelper {

    fun calculateNextDueDate(currentDueDate: Long, frequency: String?): Long? {
        if (frequency == null || frequency.lowercase() == "never") return null

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentDueDate

        when (frequency.lowercase()) {
            "daily" -> calendar.add(Calendar.DAY_OF_MONTH, 1)
            "weekly" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            "monthly" -> calendar.add(Calendar.MONTH, 1)
            "yearly" -> calendar.add(Calendar.YEAR, 1)
            else -> return null
        }

        return calendar.timeInMillis
    }

    fun shouldCreateNextInstance(frequency: String?): Boolean {
        return frequency != null && frequency.lowercase() != "never"
    }
}