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

    private val _overdueChores = MutableStateFlow<List<ChoreUI>>(emptyList())
    val overdueChores: StateFlow<List<ChoreUI>> = _overdueChores.asStateFlow()

    private val _todayChores = MutableStateFlow<List<ChoreUI>>(emptyList())
    val todayChores: StateFlow<List<ChoreUI>> = _todayChores.asStateFlow()

    private val _notes = MutableStateFlow<List<NoteUI>>(emptyList())
    val notes: StateFlow<List<NoteUI>> = _notes.asStateFlow()

    private val _housemates = MutableStateFlow<List<HousemateUI>>(emptyList())
    val housemates: StateFlow<List<HousemateUI>> = _housemates.asStateFlow()

    private val _progressData = MutableStateFlow(Triple(0, "", ""))
    val progressData: StateFlow<Triple<Int, String, String>> = _progressData.asStateFlow()

    private val _householdMemberCache = MutableStateFlow<Map<String, String>>(emptyMap())
    val householdMemberCache: StateFlow<Map<String, String>> = _householdMemberCache.asStateFlow()

    private val _mostProductiveMember = MutableStateFlow<Triple<String, String?, Boolean>>(
        Triple("Loading...", null, false)
    )
    val mostProductiveMember: StateFlow<Triple<String, String?, Boolean>> = _mostProductiveMember.asStateFlow()

    private val _recentNotesCount = MutableStateFlow(0)
    val recentNotesCount: StateFlow<Int> = _recentNotesCount.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH)

    // Store current IDs for refresh
    private var currentHouseholdId: String? = null
    private var currentUserId: String? = null

    fun loadDashboardData(householdId: String, currentUserId: String) {
        this.currentHouseholdId = householdId
        this.currentUserId = currentUserId
        
        loadAndFilterUserChores(householdId, currentUserId)
        loadRecentNotes(householdId)
        loadHousemates(householdId)
        loadMostProductiveMember(householdId)
    }

    // NEW: Call this when app resumes to refresh data
    fun refreshData() {
        val householdId = currentHouseholdId ?: return
        val userId = currentUserId ?: return
        loadDashboardData(householdId, userId)
    }

    private fun loadAndFilterUserChores(householdId: String, currentUserId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Sync from Firebase first (will update Room)
                choreRepository.syncChoresFromFirestore(householdId)
                
                // Collect from Room - this will automatically update when Room changes
                choreRepository.getActiveChoresWithRecentCompleted(householdId).collect { choreEntities ->
                    val userChores = choreEntities.filter { it.assignedTo == currentUserId }

                    val today = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }

                    val overdueList = mutableListOf<ChoreUI>()
                    val todayList = mutableListOf<ChoreUI>()

                    // Use cached member names instead of fetching each time
                    val memberCache = _householdMemberCache.value

                    userChores.forEach { chore ->
                        val assignedName = memberCache[chore.assignedTo] 
                            ?: userRepository.getUserById(chore.assignedTo).getOrNull()?.displayName
                        
                        val choreDate = Calendar.getInstance().apply {
                            timeInMillis = chore.dueDate
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }

                        val isDueToday = choreDate.timeInMillis == today.timeInMillis
                        val isOverdue = choreDate.before(today) && !chore.isCompleted

                        val choreUI = ChoreUI(
                            id = chore.id,
                            title = chore.title,
                            dueDate = dateFormat.format(Date(chore.dueDate)),
                            frequency = chore.frequency ?: "Never",
                            assignedToNames = listOfNotNull(assignedName),
                            isCompleted = chore.isCompleted,
                            isOverdue = isOverdue,
                            isSynced = chore.isSynced
                        )

                        when {
                            isOverdue -> overdueList.add(choreUI)
                            isDueToday -> todayList.add(choreUI)
                        }
                    }

                    _overdueChores.value = overdueList
                    _todayChores.value = todayList
                    updateProgress(overdueList + todayList)
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
                    val now = Calendar.getInstance()
                    val startOfWeek = Calendar.getInstance().apply {
                        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }

                    val thisWeekNotes = noteEntities.filter { it.createdAt >= startOfWeek.timeInMillis }

                    val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
                    val recentNotes = noteEntities.filter { it.createdAt >= sevenDaysAgo }
                    _recentNotesCount.value = recentNotes.size

                    val noteDateFormat = SimpleDateFormat("MMM dd yyyy", Locale.ENGLISH)
                    val memberCache = _householdMemberCache.value
                    
                    val noteUIs = thisWeekNotes.take(6).map { note ->
                        val creatorName = memberCache.values.find { 
                            memberCache.entries.find { entry -> entry.value == it }?.key == note.createdBy 
                        } ?: userRepository.getUserById(note.createdBy).getOrNull()?.displayName ?: "Unknown"
                        
                        val creator = userRepository.getUserById(note.createdBy).getOrNull()
                        val createdAtFormatted = noteDateFormat.format(Date(note.createdAt))

                        NoteUI(
                            id = note.id,
                            title = note.title,
                            content = note.content,
                            createdBy = creatorName,
                            createdAt = createdAtFormatted,
                            profilePictureUrl = creator?.profilePictureUrl,
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
                // Always refresh household data from repository (which checks local first, then Firebase)
                val householdResult = householdRepository.getHouseholdById(householdId)
                if (householdResult.isSuccess) {
                    val household = householdResult.getOrNull()
                    if (household != null) {
                        // Always rebuild the member cache
                        val memberCache = mutableMapOf<String, String>()
                        household.memberIds.forEach { userId ->
                            val userResult = userRepository.getUserById(userId)
                            val user = userResult.getOrNull()
                            if (user != null) {
                                memberCache[userId] = user.displayName
                            }
                        }
                        _householdMemberCache.value = memberCache

                        // Collect chores to calculate remaining chores per housemate
                        choreRepository.getActiveChoresByHousehold(householdId).collect { allChores ->
                            val today = Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }

                            val housemateUIs = household.memberIds.mapNotNull { userId ->
                                val userName = memberCache[userId]
                                val userResult = userRepository.getUserById(userId)
                                val user = userResult.getOrNull()

                                if (user != null) {
                                    val userChores = allChores.filter { chore ->
                                        if (chore.assignedTo != userId || chore.isCompleted) return@filter false

                                        val choreDate = Calendar.getInstance().apply {
                                            timeInMillis = chore.dueDate
                                            set(Calendar.HOUR_OF_DAY, 0)
                                            set(Calendar.MINUTE, 0)
                                            set(Calendar.SECOND, 0)
                                            set(Calendar.MILLISECOND, 0)
                                        }

                                        val isDueToday = choreDate.timeInMillis == today.timeInMillis
                                        val isOverdue = choreDate.before(today)

                                        isOverdue || isDueToday
                                    }

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
                        choreRepository.getChoresByHousehold(householdId).collect { allChores ->
                            val memberCache = _householdMemberCache.value
                            
                            val memberCompletions = household.memberIds.map { userId ->
                                val completedCount = allChores.count {
                                    it.assignedTo == userId && it.isCompleted
                                }
                                userId to completedCount
                            }.sortedByDescending { it.second }

                            when {
                                memberCompletions.isEmpty() || memberCompletions.all { it.second == 0 } -> {
                                    _mostProductiveMember.value = Triple(
                                        "No chores completed yet",
                                        null,
                                        false
                                    )
                                }
                                else -> {
                                    val topScore = memberCompletions.first().second
                                    val topMembers = memberCompletions.filter { it.second == topScore }

                                    when {
                                        topMembers.size == 1 -> {
                                            val topMemberId = topMembers.first().first
                                            val userName = memberCache[topMemberId]
                                            val user = userRepository.getUserById(topMemberId).getOrNull()
                                            _mostProductiveMember.value = Triple(
                                                userName ?: user?.displayName ?: "Unknown",
                                                user?.profilePictureUrl,
                                                true
                                            )
                                        }
                                        topMembers.size == household.memberIds.size -> {
                                            _mostProductiveMember.value = Triple(
                                                "Everyone is tied! 🎉",
                                                null,
                                                false
                                            )
                                        }
                                        else -> {
                                            val names = topMembers.mapNotNull { (userId, _) ->
                                                memberCache[userId] ?: userRepository.getUserById(userId).getOrNull()?.displayName
                                            }
                                            val displayText = when (names.size) {
                                                2 -> "${names[0]} & ${names[1]}"
                                                else -> "${names.take(2).joinToString(", ")} & ${names.size - 2} more"
                                            }
                                            _mostProductiveMember.value = Triple(
                                                "$displayText (Tied)",
                                                null,
                                                false
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message
                _mostProductiveMember.value = Triple("Error loading", null, false)
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