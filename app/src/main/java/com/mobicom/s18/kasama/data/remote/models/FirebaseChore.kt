package com.mobicom.s18.kasama.data.remote.models

import com.google.firebase.firestore.PropertyName
import com.mobicom.s18.kasama.data.local.entities.Chore

data class FirebaseChore(
    val id: String = "",
    val householdId: String = "",
    val title: String = "",
    val dueDate: Long = 0L,
    val assignedTo: String = "",
    @get:PropertyName("completed")
    @set:PropertyName("completed")
    var isCompleted: Boolean = false,
    val frequency: String? = null,
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
) {
    constructor() : this("", "", "", 0L, "")

    fun toEntity(isSynced: Boolean = true): Chore {
        return Chore(
            id = id,
            householdId = householdId,
            title = title,
            dueDate = dueDate,
            assignedTo = assignedTo,
            isCompleted = isCompleted,
            frequency = frequency,
            createdBy = createdBy,
            createdAt = createdAt,
            completedAt = completedAt,
            isSynced = isSynced,
            lastModified = System.currentTimeMillis()
        )
    }
}

// Extension function to convert Room entity back to Firebase model
fun Chore.toFirebaseModel(): FirebaseChore {
    return FirebaseChore(
        id = id,
        householdId = householdId,
        title = title,
        dueDate = dueDate,
        assignedTo = assignedTo,
        isCompleted = isCompleted,
        frequency = frequency,
        createdBy = createdBy,
        createdAt = createdAt,
        completedAt = completedAt
    )
}