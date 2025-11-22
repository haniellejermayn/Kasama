package com.mobicom.s18.kasama.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
// Import UserRepository
import com.mobicom.s18.kasama.data.repository.NoteRepository
import com.mobicom.s18.kasama.data.repository.UserRepository
import com.mobicom.s18.kasama.models.NoteUI
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NoteViewModel(
    private val noteRepository: NoteRepository,
    private val userRepository: UserRepository // <-- ADDED
) : ViewModel() {

    private val _notes = MutableStateFlow<List<NoteUI>>(emptyList())
    val notes: StateFlow<List<NoteUI>> = _notes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadNotesByHousehold(householdId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                noteRepository.syncNotesFromFirestore(householdId)
                noteRepository.getNotesByHousehold(householdId).collect { noteEntities ->
                    // val app = application as KasamaApplication // <-- REMOVED THIS LINE
                    val noteUIs = noteEntities.map { note ->
                        // Get creator name
                        // Use the userRepository from the constructor
                        val creator = userRepository.getUserById(note.createdBy).getOrNull()
                        val creatorName = creator?.displayName ?: "Unknown"

                        // Format date
                        val dateFormat = SimpleDateFormat("MMM dd yyyy", Locale.ENGLISH)
                        val createdAtFormatted = dateFormat.format(Date(note.createdAt))

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
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun createNote(householdId: String, title: String, content: String, createdBy: String, profilePictureUrl: String) {
        viewModelScope.launch {
            try {
                noteRepository.createNote(householdId, title, content, createdBy, profilePictureUrl)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun deleteNote(householdId: String, noteId: String) {
        viewModelScope.launch {
            try {
                noteRepository.deleteNote(householdId, noteId)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    class Factory(
        private val noteRepository: NoteRepository,
        private val userRepository: UserRepository // <-- ADDED
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(NoteViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return NoteViewModel(noteRepository, userRepository) as T // <-- UPDATED
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}