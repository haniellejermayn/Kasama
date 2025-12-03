package com.mobicom.s18.kasama.data.remote.models

import com.mobicom.s18.kasama.data.local.entities.User

data class FirebaseUser(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val profilePictureUrl: String? = null,
    val birthdate: Long? = null,
    val phoneNumber: String? = null,
    val householdId: String? = null,
    val fcmToken: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val householdIDs: List<String> = emptyList()
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
            fcmToken = fcmToken,
            createdAt = createdAt,
            householdIDs = householdIDs
        )
    }
}

// Extension function to convert Room entity back to Firebase model
fun User.toFirebaseModel(): FirebaseUser {
    return FirebaseUser(
        uid = uid,
        email = email,
        displayName = displayName,
        profilePictureUrl = profilePictureUrl,
        birthdate = birthdate,
        phoneNumber = phoneNumber,
        householdId = householdId,
        fcmToken = fcmToken,
        createdAt = createdAt,
        householdIDs = householdIDs
    )
}