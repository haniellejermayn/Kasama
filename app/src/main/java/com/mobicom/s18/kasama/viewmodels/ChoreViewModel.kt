package com.mobicom.s18.kasama.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mobicom.s18.kasama.data.local.KasamaDatabase
import com.mobicom.s18.kasama.data.repository.ChoreRepository
import com.mobicom.s18.kasama.data.repository.UserRepository
import com.mobicom.s18.kasama.models.ChoreUI
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ChoreViewModel(
    private val choreRepository: ChoreRepository,
    private val userRepository: UserRepository,
    private val database: KasamaDatabase
) : ViewModel() {

    data class ChoreSection(
        val userName: String,
        val userId: String,
        val chores: List<ChoreUI>,
        val isCurrentUser: Boolean
    )

    private val _chores = MutableStateFlow<List<ChoreUI>>(emptyList())
    val chores: StateFlow<List<ChoreUI>> = _chores.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _progressData = MutableStateFlow(Triple(0, 0, 0)) // completed, total, percentage
    val progressData: StateFlow<Triple<Int, Int, Int>> = _progressData.asStateFlow()

    // NEW state flow for grouped chores
    private val _choreSections = MutableStateFlow<List<ChoreSection>>(emptyList())
    val choreSections: StateFlow<List<ChoreSection>> = _choreSections.asStateFlow()

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH)

    fun loadChoresByHousehold(householdId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                choreRepository.syncChoresFromFirestore(householdId)
                choreRepository.getChoresByHousehold(householdId).collect { choreEntities ->
                    val choreUIs = choreEntities.map { chore ->
                        val assignedUser = userRepository.getUserById(chore.assignedTo).getOrNull()
                        ChoreUI(
                            id = chore.id,
                            title = chore.title,
                            dueDate = dateFormat.format(Date(chore.dueDate)),
                            frequency = chore.frequency ?: "Never",
                            assignedToNames = listOfNotNull(assignedUser?.displayName),
                            isCompleted = chore.isCompleted,
                            isSynced = chore.isSynced
                        )
                    }
                    _chores.value = choreUIs
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    // NEW function to load chores grouped by user
    fun loadChoresGroupedByUser(householdId: String, currentUserId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                choreRepository.syncChoresFromFirestore(householdId)
                choreRepository.getChoresByHousehold(householdId).collect { choreEntities ->
                    // Group chores by assignee
                    val groupedChores = choreEntities.groupBy { it.assignedTo }

                    val sections = groupedChores.map { (userId, chores) ->
                        val user = userRepository.getUserById(userId).getOrNull()
                        val choreUIs = chores.map { chore ->
                            ChoreUI(
                                id = chore.id,
                                title = chore.title,
                                dueDate = dateFormat.format(Date(chore.dueDate)),
                                frequency = chore.frequency ?: "Never",
                                assignedToNames = listOfNotNull(user?.displayName),
                                isCompleted = chore.isCompleted
                            )
                        }

                        ChoreSection(
                            userName = user?.displayName ?: "Unknown",
                            userId = userId,
                            chores = choreUIs,
                            isCurrentUser = userId == currentUserId
                        )
                    }.sortedByDescending { it.isCurrentUser } // Current user's chores first

                    _choreSections.value = sections
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    // NEW function to filter the grouped chores
    fun filterChoresByFrequencyGrouped(frequency: String): List<ChoreSection> {
        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)

        return _choreSections.value.map { section ->
            val filteredChores = section.chores.filter { chore ->
                try {
                    val dueDate = dateFormat.parse(chore.dueDate) ?: return@filter false
                    val dueCal = Calendar.getInstance().apply {
                        time = dueDate
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }

                    when (frequency.lowercase()) {
                        "today" -> isSameDay(today, dueCal)
                        "this week" -> isSameWeek(today, dueCal)
                        "this month" -> isSameMonth(today, dueCal)
                        "all" -> true
                        else -> false
                    }
                } catch (e: Exception) {
                    false
                }
            }

            section.copy(chores = filteredChores)
        }.filter { it.chores.isNotEmpty() } // Only show sections with chores
    }

    // NEW function to calculate progress for the grouped/filtered view
    fun calculateProgressForFilter(frequency: String) {
        val sections = filterChoresByFrequencyGrouped(frequency)
        val allChores = sections.flatMap { it.chores }
        val completed = allChores.count { it.isCompleted }
        val total = allChores.size
        val percentage = if (total > 0) (completed * 100 / total) else 0
        _progressData.value = Triple(completed, total, percentage)
    }

    fun filterChoresByFrequency(frequency: String): List<ChoreUI> {
        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)

        return _chores.value.filter { chore ->
            try {
                val dueDate = dateFormat.parse(chore.dueDate) ?: return@filter false
                val dueCal = Calendar.getInstance().apply {
                    time = dueDate
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                when (frequency.lowercase()) {
                    "daily" -> isSameDay(today, dueCal) // "daily" might mean "today"
                    "weekly" -> isSameWeek(today, dueCal)
                    "monthly" -> isSameMonth(today, dueCal)
                    else -> false
                }
            } catch (e: Exception) {
                false
            }
        }
    }

    fun calculateProgress(frequency: String) {
        val filteredChores = filterChoresByFrequency(frequency)
        val completed = filteredChores.count { it.isCompleted }
        val total = filteredChores.size
        val percentage = if (total > 0) (completed * 100 / total) else 0
        _progressData.value = Triple(completed, total, percentage)
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

    private fun isSameDay(c1: Calendar, c2: Calendar): Boolean {
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isSameWeek(c1: Calendar, c2: Calendar): Boolean {
        val startOfWeek = c1.clone() as Calendar
        startOfWeek.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        startOfWeek.set(Calendar.HOUR_OF_DAY, 0)
        startOfWeek.set(Calendar.MINUTE, 0)
        startOfWeek.set(Calendar.SECOND, 0)

        val endOfWeek = startOfWeek.clone() as Calendar
        endOfWeek.add(Calendar.DAY_OF_MONTH, 6)

        return c2.timeInMillis >= startOfWeek.timeInMillis &&
                c2.timeInMillis <= endOfWeek.timeInMillis
    }

    private fun isSameMonth(c1: Calendar, c2: Calendar): Boolean {
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH)
    }

    class Factory(
        private val choreRepository: ChoreRepository,
        private val userRepository: UserRepository,
        private val database: KasamaDatabase
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ChoreViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ChoreViewModel(choreRepository, userRepository, database) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}