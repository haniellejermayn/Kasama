package com.mobicom.s18.kasama.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_deletes")
data class PendingDelete(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: String, // The ID of the deleted chore/note
    val itemType: String, // "chore" or "note"
    val householdId: String,
    val createdAt: Long = System.currentTimeMillis()
)