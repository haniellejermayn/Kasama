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

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH)

    fun loadDashboardData(householdId: String) {
        loadChores(householdId)
        loadNotes(householdId)
        loadHousemates(householdId)
    }

    private fun loadChores(householdId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                choreRepository.syncChoresFromFirestore(householdId)
                choreRepository.getChoresByHousehold(householdId).collect { choreEntities ->
                    val choreUIs = choreEntities.take(4).map { chore ->
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
                    updateProgress(choreEntities.map { chore ->
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

    private fun loadNotes(householdId: String) {
        viewModelScope.launch {
            try {
                noteRepository.syncNotesFromFirestore(householdId)
                noteRepository.getNotesByHousehold(householdId).collect { noteEntities ->
                    val noteUIs = noteEntities.take(6).map { note ->
                        NoteUI(
                            id = note.id,
                            title = note.title,
                            content = note.content
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
                                // Get chores from Room database for this user
                                val allChores = choreRepository.getChoresByHousehold(householdId).first()
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

    fun toggleChoreCompletion(choreId: String, householdId: String) {
        viewModelScope.launch {
            try {
                // Get the chore entity from Room database
                val choreEntity = database.choreDao().getChoreByIdOnce(choreId) ?: return@launch

                // Toggle completion status
                val updatedChore = choreEntity.copy(
                    isCompleted = !choreEntity.isCompleted,
                    completedAt = if (!choreEntity.isCompleted) System.currentTimeMillis() else null
                )

                // Convert to FirebaseChore and update
                choreRepository.updateChore(
                    com.mobicom.s18.kasama.data.remote.models.FirebaseChore(
                        id = updatedChore.id,
                        householdId = updatedChore.householdId,
                        title = updatedChore.title,
                        dueDate = updatedChore.dueDate,
                        assignedTo = updatedChore.assignedTo,
                        isCompleted = updatedChore.isCompleted,
                        frequency = updatedChore.frequency,
                        createdBy = updatedChore.createdBy,
                        createdAt = updatedChore.createdAt,
                        completedAt = updatedChore.completedAt
                    )
                )
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
            progressPercentage >= 50 -> "You're halfway through your chores!"
            progressPercentage > 0 -> "You have completed $progressPercentage% of your chores!"
            else -> "Let's get started on those chores!"
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