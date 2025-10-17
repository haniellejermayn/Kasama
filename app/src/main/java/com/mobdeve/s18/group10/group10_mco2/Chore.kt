package com.mobdeve.s18.group10.group10_mco2

data class Chore(
    val title: String,
    val dueDate: String,
    val repeats: String,
    val assignedTo: String,
    var isCompleted: Boolean
)
