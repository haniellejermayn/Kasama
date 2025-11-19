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
                choreRepository.getActiveChoresWithRecentCompleted(householdId).collect { choreEntities ->
                    val userChores = choreEntities.filter { it.assignedTo == currentUserId }

                    val today = Calendar.getInstance()
                    today.set(Calendar.HOUR_OF_DAY, 0)
                    today.set(Calendar.MINUTE, 0)
                    today.set(Calendar.SECOND, 0)
                    today.set(Calendar.MILLISECOND, 0)

                    // DASHBOARD FILTERING RULES:
                    // 1. Show incomplete chores that are overdue OR due today
                    // 2. Show completed chores that are due today only
                    val filteredChores = userChores.filter { chore ->
                        val choreDate = Calendar.getInstance().apply {
                            timeInMillis = chore.dueDate
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }

                        val isDueToday = choreDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                                choreDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
                        val isOverdue = choreDate.before(today)

                        when {
                            // Completed chores: only show if due today
                            chore.isCompleted -> isDueToday
                            // Incomplete chores: show if overdue OR due today
                            else -> isOverdue || isDueToday
                        }
                    }

                    val choreUIs = filteredChores.map { chore ->
                        val assignedUser = userRepository.getUserById(chore.assignedTo).getOrNull()
                        val choreDate = Calendar.getInstance().apply {
                            timeInMillis = chore.dueDate
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        val isOverdue = choreDate.before(today) && !chore.isCompleted

                        ChoreUI(
                            id = chore.id,
                            title = chore.title,
                            dueDate = dateFormat.format(Date(chore.dueDate)),
                            frequency = chore.frequency ?: "Never",
                            assignedToNames = listOfNotNull(assignedUser?.displayName),
                            isCompleted = chore.isCompleted,
                            isOverdue = isOverdue
                        )
                    }

                    _chores.value = choreUIs

                    // Update progress based on all filtered chores
                    updateProgress(choreUIs)
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
                    // Calculate start of current week (Monday)
                    val now = Calendar.getInstance()
                    val startOfWeek = Calendar.getInstance().apply {
                        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }

                    // Filter notes from this week
                    val thisWeekNotes = noteEntities.filter { it.createdAt >= startOfWeek.timeInMillis }

                    // Update recent notes count (last 7 days for the info card)
                    val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
                    val recentNotes = noteEntities.filter { it.createdAt >= sevenDaysAgo }
                    _recentNotesCount.value = recentNotes.size

                    // Format notes for display
                    val noteDateFormat = SimpleDateFormat("MMM dd", Locale.ENGLISH)
                    val noteUIs = thisWeekNotes.take(6).map { note ->
                        val creator = userRepository.getUserById(note.createdBy).getOrNull()
                        val creatorName = creator?.displayName ?: "Unknown"
                        val createdAtFormatted = noteDateFormat.format(Date(note.createdAt))

                        NoteUI(
                            id = note.id,
                            title = note.title,
                            content = note.content,
                            createdBy = creatorName,
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