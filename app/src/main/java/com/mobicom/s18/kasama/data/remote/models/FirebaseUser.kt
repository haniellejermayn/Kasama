package com.mobicom.s18.kasama.data.remote.models

data class FirebaseUser(
    val uid: String = " ",
    val email: String = " ",
    val displayName: String = " ",
    val profilePictureUrl: String? = null,
    val birthdate: Long? = null,
    val phoneNumber: String? = null,
    val householdId: String? = null,  // Single household for now
    val createdAt: Long = System.currentTimeMillis()
) {
    constructor() : this("", "", "")

    // convert to room entity
    fun toEntity():
}
