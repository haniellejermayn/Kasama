package com.mobicom.s18.kasama

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.mobicom.s18.kasama.databinding.LayoutNotesPageBinding
import com.mobicom.s18.kasama.viewmodels.NoteViewModel
import kotlinx.coroutines.launch

class NoteActivity : AppCompatActivity() {
    private lateinit var binding: LayoutNotesPageBinding
    private lateinit var noteAdapter: NoteAdapter

    private val viewModel: NoteViewModel by viewModels {
        val app = application as KasamaApplication
        NoteViewModel.Factory(
            app.noteRepository,
            app.userRepository // <-- ADDED
        )
    }

    private var currentHouseholdId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = LayoutNotesPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        observeViewModel()
        loadUserHousehold()
    }

    private fun loadUserHousehold() {
        lifecycleScope.launch {
            val app = application as KasamaApplication
            val currentUser = app.firebaseAuth.currentUser
            if (currentUser != null) {
                val userResult = app.userRepository.getUserById(currentUser.uid)
                if (userResult.isSuccess) {
                    val user = userResult.getOrNull()
                    currentHouseholdId = user?.householdId
                    currentHouseholdId?.let { householdId ->
                        viewModel.loadNotesByHousehold(householdId)
                    }
                }
            }
        }
    }

    private fun setupRecyclerView() {
        noteAdapter = NoteAdapter(mutableListOf())
        binding.rvNotes.layoutManager = GridLayoutManager(this, 3)
        binding.rvNotes.adapter = noteAdapter
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.notes.collect { notes ->
                noteAdapter = NoteAdapter(notes.toMutableList())
                binding.rvNotes.adapter = noteAdapter
            }
        }
    }
}