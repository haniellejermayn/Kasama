package com.mobicom.s18.kasama.data.remote.models

import com.mobicom.s18.kasama.data.local.entities.Household

data class FirebaseHousehold(
    val id: String = "",
    val name: String = "",
    val inviteCode: String = "",
    val createdBy: String = "",
    val memberIds: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
) {
    constructor() : this("", "", "", "")

    fun toEntity(): Household {
        return Household(
            id = id,
            name = name,
            inviteCode = inviteCode,
            createdBy = createdBy,
            createdAt = createdAt,
            memberIds = memberIds
        )
    }
}