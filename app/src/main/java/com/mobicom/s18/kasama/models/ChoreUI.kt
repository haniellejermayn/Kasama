package com.mobicom.s18.kasama.models

data class ChoreUI(
    val id: String,
    val title: String,
    val dueDate: String,
    val frequency: String,
    val assignedToNames: List<String>,
    var isCompleted: Boolean,
    val isSynced: Boolean = true,
    val isOverdue: Boolean = false
)