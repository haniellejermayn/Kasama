package com.mobicom.s18.kasama.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey val id: String,
    val householdId: String,
    val title: String,
    val content: String,
    val createdBy: String,
    val profilePictureUrl: String?,
    val createdAt: Long = System.currentTimeMillis(),

    // Sync tracking fields
    val isSynced: Boolean = false,
    val lastModified: Long = System.currentTimeMillis(),
)