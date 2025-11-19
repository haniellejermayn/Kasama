package com.mobicom.s18.kasama.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mobicom.s18.kasama.data.local.KasamaDatabase
import com.mobicom.s18.kasama.data.repository.ChoreRepository
import com.mobicom.s18.kasama.data.repository.NoteRepository
import com.mobicom.s18.kasama.data.repository.UserRepository
import com.mobicom.s18.kasama.data.repository.HouseholdRepository
import com.mobicom.s18.kasama.models.ChoreUI
import com.mobicom.s18.kasama.models.NoteUI
import com.mobicom.s18.kasama.models.HousemateUI
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DashboardViewModel(
    private val choreRepository: ChoreRepository,
    private val noteRepository: NoteRepository,
    private val userRepository: UserRepository,
    private val householdRepository: HouseholdRepository,
    private val database: KasamaDatabase
) : ViewModel() {

    private val _chores = MutableStateFlow<List<ChoreUI>>(emptyList())
    val chores: StateFlow<List<ChoreUI>> = _chores.asStateFlow()

    private val _notes = MutableStateFlow<List<NoteUI>>(emptyList())
    val notes: StateFlow<List<NoteUI>> = _notes.asStateFlow()

    private val _housemates = MutableStateFlow<List<HousemateUI>>(emptyList())
    val housemates: StateFlow<List<HousemateUI>> = _housemates.asStateFlow()

    private val _progressData = MutableStateFlow(Triple(0, "", "")) // percentage, message, progress text
    val progressData: StateFlow<Triple<Int, String, String>> = _progressData.asStateFlow()

    private val _mostProductiveMember = MutableStateFlow<Pair<String, String?>>(Pair("Loading...", null)) // name, profileUrl
    val mostProductiveMember: StateFlow<Pair<String, String?>> = _mostProductiveMember.asStateFlow()

    private val _recentNotesCount = MutableStateFlow(0)
    val recentNotesCount: StateFlow<Int> = _recentNotesCount.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH)

    fun loadDashboardData(householdId: String, currentUserId: String) {
        loadUserChores(householdId, currentUserId)
        loadRecentNotes(householdId)
        loadHousemates(householdId)
        loadMostProductiveMember(householdId)
    }

    private fun loadUserChores(householdId: String, currentUserId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                choreRepository.syncChoresFromFirestore(householdId)
                choreRepository.getActiveChoresByHousehold(householdId).collect { choreEntities ->
                    // filter to only show current user's chores
                    val userChores = choreEntities.filter { it.assignedTo == currentUserId }

                    val choreUIs = userChores.take(4).map { chore ->
                        val assignedUser = userRepository.getUserById(chore.assignedTo).getOrNull()
                        ChoreUI(
                            id = chore.id,
                            title = chore.title,
                            dueDate = dateFormat.format(Date(chore.dueDate)),
                            frequency = chore.frequency ?: "Never",
                            assignedToNames = listOfNotNull(assignedUser?.displayName),
                            isCompleted = chore.isCompleted
                        )
                    }
                    _chores.value = choreUIs

                    // Update progress based on ALL user chores (not just top 4)
                    updateProgress(userChores.map { chore ->
                        ChoreUI(
                            id = chore.id,
                            title = chore.title,
                            dueDate = dateFormat.format(Date(chore.dueDate)),
                            frequency = chore.frequency ?: "Never",
                            assignedToNames = emptyList(),
                            isCompleted = chore.isCompleted
                        )
                    })
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    private fun loadRecentNotes(householdId: String) {
        viewModelScope.launch {
            try {
                noteRepository.syncNotesFromFirestore(householdId)
                noteRepository.getNotesByHousehold(householdId).collect { noteEntities ->
                    // Get notes from the last 7 days
                    val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
                    val recentNotes = noteEntities.filter { it.createdAt >= sevenDaysAgo }

                    _recentNotesCount.value = recentNotes.size

                    // Define a date formatter
                    val noteDateFormat = SimpleDateFormat("MMM dd", Locale.ENGLISH)

                    val noteUIs = noteEntities.take(6).map { note ->
                        // --- THIS IS THE FIX ---
                        // 1. Get creator name
                        val creator = userRepository.getUserById(note.createdBy).getOrNull()
                        val creatorName = creator?.displayName ?: "Unknown"

                        // 2. Format date
                        val createdAtFormatted = noteDateFormat.format(Date(note.createdAt))

                        // 3. Create the complete NoteUI object
                        NoteUI(
                            id = note.id,
                            title = note.title,
                            content = note.content,
                            createdBy = creatorName, // <-- Author is now included
                            createdAt = createdAtFormatted,
                            isSynced = note.isSynced
                        )
                    }
                    _notes.value = noteUIs
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    private fun loadHousemates(householdId: String) {
        viewModelScope.launch {
            try {
                val householdResult = householdRepository.getHouseholdById(householdId)
                if (householdResult.isSuccess) {
                    val household = householdResult.getOrNull()
                    if (household != null) {
                        val housemateUIs = household.memberIds.mapNotNull { userId ->
                            val userResult = userRepository.getUserById(userId)
                            val user = userResult.getOrNull()

                            if (user != null) {
                                val allChores = choreRepository.getActiveChoresByHousehold(householdId).first()
                                val userChores = allChores.filter { it.assignedTo == userId && !it.isCompleted }

                                HousemateUI(
                                    id = user.uid,
                                    name = user.displayName,
                                    choresRemaining = userChores.size,
                                    profilePictureUrl = user.profilePictureUrl
                                )
                            } else null
                        }
                        _housemates.value = housemateUIs
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    private fun loadMostProductiveMember(householdId: String) {
        viewModelScope.launch {
            try {
                val householdResult = householdRepository.getHouseholdById(householdId)
                if (householdResult.isSuccess) {
                    val household = householdResult.getOrNull()
                    if (household != null) {
                        val allChores = choreRepository.getChoresByHousehold(householdId).first()

                        // Calculate completion count for each member
                        val memberCompletions = household.memberIds.map { userId ->
                            val completedCount = allChores.count {
                                it.assignedTo == userId && it.isCompleted
                            }
                            userId to completedCount
                        }.sortedByDescending { it.second }

                        // Get the most productive member
                        val topMemberId = memberCompletions.firstOrNull()?.first
                        if (topMemberId != null) {
                            val user = userRepository.getUserById(topMemberId).getOrNull()
                            _mostProductiveMember.value = Pair(
                                user?.displayName ?: "Unknown",
                                user?.profilePictureUrl
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message
                _mostProductiveMember.value = Pair("Error loading", null)
            }
        }
    }

    fun toggleChoreCompletion(choreId: String, householdId: String) {
        viewModelScope.launch {
            try {
                val choreEntity = database.choreDao().getChoreByIdOnce(choreId) ?: return@launch
                if (choreEntity.isCompleted) {
                    choreRepository.uncompleteChore(choreId)
                } else {
                    choreRepository.completeChore(choreId)
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    private fun updateProgress(allChores: List<ChoreUI>) {
        val completedChores = allChores.count { it.isCompleted }
        val totalChores = allChores.size
        val progressPercentage = if (totalChores > 0) {
            (completedChores.toFloat() / totalChores.toFloat() * 100).toInt()
        } else {
            0
        }

        val message = when {
            progressPercentage == 100 -> "You've completed all your chores!"
            progressPercentage >= 75 -> "Almost there! Keep it up!"
            progressPercentage >= 50 -> "You're halfway through!"
            progressPercentage > 0 -> "You've completed $progressPercentage% of your chores!"
            else -> "Let's get started!"
        }

        val progressText = "$completedChores / $totalChores"

        _progressData.value = Triple(progressPercentage, message, progressText)
    }

    class Factory(
        private val choreRepository: ChoreRepository,
        private val noteRepository: NoteRepository,
        private val userRepository: UserRepository,
        private val householdRepository: HouseholdRepository,
        private val database: KasamaDatabase
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return DashboardViewModel(choreRepository, noteRepository, userRepository, householdRepository, database) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}