package com.mobdeve.s18.group10.group10_mco2

class Chore(
    var title: String,
    var dueDate: String,
    var frequency: String,
    var assignedToList: List<String>,
    var isCompleted: Boolean = false
) {
    // Helper function to get assignees as a formatted string
    fun getAssigneesString(): String {
        return when (assignedToList.size) {
            0 -> "Unassigned"
            1 -> assignedToList[0]
            2 -> "${assignedToList[0]} and ${assignedToList[1]}"
            else -> "${assignedToList[0]} and ${assignedToList.size - 1} others"
        }
    }
}