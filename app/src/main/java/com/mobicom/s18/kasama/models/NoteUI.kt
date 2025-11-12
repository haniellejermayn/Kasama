package com.mobicom.s18.kasama.models

data class NoteUI(
    val id: String,
    val title: String,
    val content: String,
    val createdBy: String = "",
    val createdAt: String = "", // (formatted date)
    val isSynced: Boolean = true
)