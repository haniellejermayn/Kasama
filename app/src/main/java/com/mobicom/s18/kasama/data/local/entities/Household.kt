package com.mobicom.s18.kasama.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "households")
data class Household(
    @PrimaryKey val id: String,
    val name: String,
    val inviteCode: String,
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis(),
    val memberIds: List<String> = emptyList()
)