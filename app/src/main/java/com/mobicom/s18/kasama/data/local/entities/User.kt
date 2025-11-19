package com.mobicom.s18.kasama.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val uid: String,
    val email: String,
    val displayName: String,
    val profilePictureUrl: String? = null,
    val birthdate: Long? = null,
    val phoneNumber: String? = null,
    val householdId: String? = null,
    val fcmToken: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val householdIDs: List<String> = emptyList()
)