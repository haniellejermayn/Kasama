package com.mobicom.s18.kasama.utils

import android.R
import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.mobicom.s18.kasama.KasamaApplication
import com.mobicom.s18.kasama.databinding.LayoutBottomNoteDetailBinding
import com.mobicom.s18.kasama.models.NoteUI
import kotlinx.coroutines.launch

fun showNoteBottomSheet(
    context: Context,
    householdId: String,
    currentUserId: String,
    profilePictureUrl: String?,
    note: NoteUI? = null,
    onSave: () -> Unit = {}
) {
    val bottomSheet = BottomSheetDialog(context)
    bottomSheet.behavior.isFitToContents = true
    val binding = LayoutBottomNoteDetailBinding.inflate(LayoutInflater.from(context))

    val app = (context.applicationContext as KasamaApplication)

    binding.editTextTitle.setText(note?.title ?: "")
    binding.editTextContent.setText(note?.content ?: "")

    // Show delete button only when editing
    if (note != null) {
        binding.buttonDelete.visibility = View.VISIBLE
    } else {
        binding.buttonDelete.visibility = View.GONE
    }

    binding.buttonDelete.setOnClickListener {
        AlertDialog.Builder(context)
            .setTitle("Delete Note")
            .setMessage("Are you sure you want to delete this note?")
            .setPositiveButton("Delete") { _, _ ->
                note?.let { noteToDelete ->
                    (context as? LifecycleOwner)?.lifecycleScope?.launch {
                        val result = app.noteRepository.deleteNote(householdId, noteToDelete.id)
                        if (result.isSuccess) {
                            Toast.makeText(context, "Note deleted", Toast.LENGTH_SHORT).show()
                            onSave()
                            bottomSheet.dismiss()
                        } else {
                            Toast.makeText(context, "Failed to delete note", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    binding.buttonCancel.setOnClickListener {
        bottomSheet.dismiss()
    }

    binding.buttonSave.setOnClickListener {
        val title = binding.editTextTitle.text.toString().trim()
        val content = binding.editTextContent.text.toString().trim()

        if (title.isEmpty() && content.isEmpty()) {
            Toast.makeText(context, "Please enter something.", Toast.LENGTH_SHORT).show()
            return@setOnClickListener
        }

        (context as? LifecycleOwner)?.lifecycleScope?.launch {
            try {
                if (note == null) {
                    // Create new note
                    val result = app.noteRepository.createNote(
                        householdId = householdId,
                        title = title.ifEmpty { "Untitled" },
                        content = content,
                        createdBy = currentUserId,
                        profilePictureUrl = profilePictureUrl
                    )

                    if (result.isSuccess) {
                        Toast.makeText(context, "Note created!", Toast.LENGTH_SHORT).show()
                        onSave()
                        bottomSheet.dismiss()
                    } else {
                        Toast.makeText(context, "Failed to create note", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Update existing note
                    val noteEntity = app.database.noteDao().getNoteByIdOnce(note.id)
                    if (noteEntity != null) {
                        val updatedNote = com.mobicom.s18.kasama.data.remote.models.FirebaseNote(
                            id = noteEntity.id,
                            householdId = noteEntity.householdId,
                            title = title.ifEmpty { "Untitled" },
                            content = content,
                            createdBy = noteEntity.createdBy,
                            createdAt = noteEntity.createdAt
                        )

                        val result = app.noteRepository.updateNote(updatedNote)
                        if (result.isSuccess) {
                            Toast.makeText(context, "Note updated!", Toast.LENGTH_SHORT).show()
                            onSave()
                            bottomSheet.dismiss()
                        } else {
                            Toast.makeText(context, "Failed to update note", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    bottomSheet.window?.setBackgroundDrawableResource(R.color.transparent)
    bottomSheet.setContentView(binding.root)
    bottomSheet.show()
}