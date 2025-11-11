package com.mobicom.s18.kasama.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mobicom.s18.kasama.data.repository.NoteRepository
import com.mobicom.s18.kasama.models.NoteUI
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NoteViewModel(
    private val noteRepository: NoteRepository
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
                    val noteUIs = noteEntities.map { note ->
                        NoteUI(
                            id = note.id,
                            title = note.title,
                            content = note.content
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

    fun createNote(householdId: String, title: String, content: String, createdBy: String) {
        viewModelScope.launch {
            try {
                noteRepository.createNote(householdId, title, content, createdBy)
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
        private val noteRepository: NoteRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(NoteViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return NoteViewModel(noteRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}