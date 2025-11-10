package com.mobicom.s18.kasama.data.remote.models

import com.mobicom.s18.kasama.data.local.entities.User

data class FirebaseUser(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val profilePictureUrl: String? = null,
    val birthdate: Long? = null,
    val phoneNumber: String? = null,
    val householdId: String? = null,  // Single household for now
    val createdAt: Long = System.currentTimeMillis()
) {
    // required for firestore
    constructor() : this("", "", "")

    // convert to room entity
    fun toEntity(): User {
        return User(
            uid = uid,
            email = email,
            displayName = displayName,
            profilePictureUrl = profilePictureUrl,
            birthdate = birthdate,
            phoneNumber = phoneNumber,
            householdId = householdId,
            createdAt = createdAt
        )
    }
}