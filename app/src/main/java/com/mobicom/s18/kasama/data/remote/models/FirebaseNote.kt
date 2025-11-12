package com.mobicom.s18.kasama.data.remote.models

import com.mobicom.s18.kasama.data.local.entities.Note

data class FirebaseNote(
    val id: String = "",
    val householdId: String = "",
    val title: String = "",
    val content: String = "",
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    constructor() : this("", "", "", "", "")

    fun toEntity(isSynced: Boolean = true): Note {
        return Note(
            id = id,
            householdId = householdId,
            title = title,
            content = content,
            createdBy = createdBy,
            createdAt = createdAt,
            isSynced = isSynced,
            lastModified = System.currentTimeMillis()
        )
    }
}

// Extension function to convert Room entity back to Firebase model
fun Note.toFirebaseModel(): FirebaseNote {
    return FirebaseNote(
        id = id,
        householdId = householdId,
        title = title,
        content = content,
        createdBy = createdBy,
        createdAt = createdAt
    )
}