package com.mobicom.s18.kasama.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chores")
data class Chore(
    @PrimaryKey val id: String,
    val householdId: String,
    val title: String,
    val dueDate: Long,
    val assignedTo: String, // User ID
    val isCompleted: Boolean = false,
    val frequency: String? = null, // "daily", "weekly", "monthly", or null
    val createdBy: String, // User ID
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)
