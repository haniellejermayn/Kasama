package com.mobicom.s18.kasama.utils

import android.R
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.mobicom.s18.kasama.KasamaApplication
import com.mobicom.s18.kasama.databinding.LayoutBottomChoreDetailBinding
import com.mobicom.s18.kasama.models.ChoreUI
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

fun showChoreBottomSheet(
    context: Context,
    availableHousemates: List<String>,
    householdId: String,
    currentUserId: String,
    chore: ChoreUI? = null,
    onSave: () -> Unit = {}
) {
    val bottomSheet = BottomSheetDialog(context)
    bottomSheet.behavior.isFitToContents = true
    val binding = LayoutBottomChoreDetailBinding.inflate(LayoutInflater.from(context))

    val app = (context.applicationContext as KasamaApplication)
    var selectedAssignee: String? = null
    var selectedAssigneeId: String? = null
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH)

    // Pre-fill if editing
    chore?.let {
        binding.editTextChoreTitle.setText(it.title)
        binding.textDueDate.text = it.dueDate

        if (it.assignedToNames.isNotEmpty()) {
            selectedAssignee = it.assignedToNames[0]
            binding.textAssignedPerson.text = selectedAssignee
            binding.textAssignedPerson.setTextColor(Color.BLACK)

            // Get the user ID for the assigned person
            (context as? LifecycleOwner)?.lifecycleScope?.launch {
                val household = app.householdRepository.getHouseholdById(householdId).getOrNull()
                household?.memberIds?.forEach { userId ->
                    val user = app.userRepository.getUserById(userId).getOrNull()
                    if (user?.displayName == selectedAssignee) {
                        selectedAssigneeId = userId
                    }
                }
            }
        }

        // Show delete button only when editing
        binding.buttonDelete.visibility = android.view.View.VISIBLE
    } ?: run {
        binding.buttonDelete.visibility = android.view.View.GONE
    }

    if (chore == null) {
        val calendar = Calendar.getInstance()
        binding.textDueDate.text = dateFormat.format(calendar.time)
    }

    val repeatOptions = arrayOf("Never", "Daily", "Weekly", "Monthly", "Yearly")
    val adapter = ArrayAdapter(context, R.layout.simple_spinner_item, repeatOptions)
    adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
    binding.spinnerRepeats.adapter = adapter

    chore?.let {
        val position = repeatOptions.indexOf(it.frequency)
        if (position >= 0) binding.spinnerRepeats.setSelection(position)
    }

    binding.textDueDate.setOnClickListener {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                binding.textDueDate.text = dateFormat.format(calendar.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    binding.textAssignedPerson.setOnClickListener {
        (context as? LifecycleOwner)?.lifecycleScope?.launch {
            val household = app.householdRepository.getHouseholdById(householdId).getOrNull()
            if (household != null) {
                val housemateMap = mutableMapOf<String, String>() // name to id mapping
                household.memberIds.forEach { userId ->
                    val user = app.userRepository.getUserById(userId).getOrNull()
                    if (user != null) {
                        housemateMap[user.displayName] = userId
                    }
                }

                showSingleHousemateSelectionDialog(
                    context,
                    housemateMap.keys.toList(),
                    selectedAssignee
                ) { selected ->
                    selectedAssignee = selected
                    selectedAssigneeId = housemateMap[selected]
                    binding.textAssignedPerson.text = selected
                    binding.textAssignedPerson.setTextColor(Color.BLACK)
                }
            }
        }
    }

    binding.buttonDelete.setOnClickListener {
        AlertDialog.Builder(context)
            .setTitle("Delete Chore")
            .setMessage("Are you sure you want to delete this chore?")
            .setPositiveButton("Delete") { _, _ ->
                chore?.let { choreToDelete ->
                    (context as? LifecycleOwner)?.lifecycleScope?.launch {
                        val result = app.choreRepository.deleteChore(householdId, choreToDelete.id)
                        if (result.isSuccess) {
                            Toast.makeText(context, "Chore deleted", Toast.LENGTH_SHORT).show()
                            onSave()
                            bottomSheet.dismiss()
                        } else {
                            Toast.makeText(context, "Failed to delete chore", Toast.LENGTH_SHORT).show()
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
        val title = binding.editTextChoreTitle.text.toString().trim()
        val dueDateText = binding.textDueDate.text.toString()
        val repeats = binding.spinnerRepeats.selectedItem.toString()

        when {
            title.isEmpty() -> {
                Toast.makeText(context, "Please enter a chore title", Toast.LENGTH_SHORT).show()
            }
            selectedAssigneeId == null -> {
                Toast.makeText(context, "Please assign to a person", Toast.LENGTH_SHORT).show()
            }
            dueDateText.isBlank() -> {
                Toast.makeText(context, "Please select a due date", Toast.LENGTH_SHORT).show()
            }
            else -> {
                (context as? LifecycleOwner)?.lifecycleScope?.launch {
                    try {
                        val dueDate = dateFormat.parse(dueDateText)?.time ?: System.currentTimeMillis()
                        val frequency = if (repeats == "Never") null else repeats.lowercase()

                        if (chore == null) {
                            // Create new chore
                            val result = app.choreRepository.createChore(
                                householdId = householdId,
                                title = title,
                                dueDate = dueDate,
                                assignedTo = selectedAssigneeId!!,
                                frequency = frequency,
                                createdBy = currentUserId
                            )

                            if (result.isSuccess) {
                                Toast.makeText(context, "Chore created: $title", Toast.LENGTH_SHORT).show()
                                onSave()
                                bottomSheet.dismiss()
                            } else {
                                Toast.makeText(context, "Failed to create chore", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            // Update existing chore
                            val choreEntity = app.database.choreDao().getChoreByIdOnce(chore.id)
                            if (choreEntity != null) {
                                val updatedChore = com.mobicom.s18.kasama.data.remote.models.FirebaseChore(
                                    id = choreEntity.id,
                                    householdId = choreEntity.householdId,
                                    title = title,
                                    dueDate = dueDate,
                                    assignedTo = selectedAssigneeId!!,
                                    isCompleted = choreEntity.isCompleted,
                                    frequency = frequency,
                                    createdBy = choreEntity.createdBy,
                                    createdAt = choreEntity.createdAt,
                                    completedAt = choreEntity.completedAt
                                )

                                val result = app.choreRepository.updateChore(updatedChore)
                                if (result.isSuccess) {
                                    Toast.makeText(context, "Chore updated: $title", Toast.LENGTH_SHORT).show()
                                    onSave()
                                    bottomSheet.dismiss()
                                } else {
                                    Toast.makeText(context, "Failed to update chore", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    bottomSheet.window?.setBackgroundDrawableResource(R.color.transparent)
    bottomSheet.setContentView(binding.root)
    bottomSheet.show()
}

private fun showSingleHousemateSelectionDialog(
    context: Context,
    availableHousemates: List<String>,
    currentSelected: String?,
    onSelected: (String) -> Unit
) {
    val currentIndex = if (currentSelected != null) {
        availableHousemates.indexOf(currentSelected)
    } else {
        -1
    }

    AlertDialog.Builder(context)
        .setTitle("Assign to Housemate")
        .setSingleChoiceItems(
            availableHousemates.toTypedArray(),
            currentIndex
        ) { dialog, which ->
            onSelected(availableHousemates[which])
            dialog.dismiss()
        }
        .setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        .show()
}