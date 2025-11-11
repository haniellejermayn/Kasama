package com.mobicom.s18.kasama.utils

import android.R
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.mobicom.s18.kasama.databinding.LayoutBottomNoteDetailBinding
import com.mobicom.s18.kasama.models.NoteUI
import android.view.View

fun showNoteBottomSheet(
    context: Context,
    note: NoteUI? = null,
    onSave: (title: String, content: String) -> Unit
) {
    val bottomSheet = BottomSheetDialog(context)
    bottomSheet.behavior.isFitToContents = true
    val binding = LayoutBottomNoteDetailBinding.inflate(LayoutInflater.from(context))

    binding.editTextTitle.setText(note?.title ?: "")
    binding.editTextContent.setText(note?.content ?: "")

    binding.buttonCancel.setOnClickListener { bottomSheet.dismiss() }
    binding.buttonSave.setOnClickListener {
        val title = binding.editTextTitle.text.toString().trim()
        val content = binding.editTextContent.text.toString().trim()

        if (title.isNotEmpty() || content.isNotEmpty()) {
            onSave(title, content)
            Toast.makeText(context, "Note saved!", Toast.LENGTH_SHORT).show()
            bottomSheet.dismiss()
        } else {
            Toast.makeText(context, "Please enter something.", Toast.LENGTH_SHORT).show()
        }
    }

    bottomSheet.window?.setBackgroundDrawableResource(R.color.transparent)
    bottomSheet.setContentView(binding.root)
    bottomSheet.show()
}