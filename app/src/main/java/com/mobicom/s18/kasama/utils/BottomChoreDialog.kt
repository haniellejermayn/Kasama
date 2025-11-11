package com.mobicom.s18.kasama.utils

import android.R
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.mobicom.s18.kasama.databinding.LayoutBottomChoreDetailBinding
import com.mobicom.s18.kasama.models.ChoreUI
import java.text.SimpleDateFormat
import java.util.*

fun showChoreBottomSheet(
    context: Context,
    availableHousemates: List<String>,
    chore: ChoreUI? = null,
    onSave: (ChoreUI) -> Unit = {}
) {
    val bottomSheet = BottomSheetDialog(context)
    bottomSheet.behavior.isFitToContents = true
    val binding = LayoutBottomChoreDetailBinding.inflate(LayoutInflater.from(context))

    // CHANGED: Single assignee instead of list
    var selectedAssignee: String? = null

    // Pre-fill if editing
    chore?.let {
        binding.editTextChoreTitle.setText(it.title)
        binding.textDueDate.text = it.dueDate

        // CHANGED: Get first assignee (single assignment)
        if (it.assignedToNames.isNotEmpty()) {
            selectedAssignee = it.assignedToNames[0]
            binding.textAssignedPerson.text = selectedAssignee
            binding.textAssignedPerson.setTextColor(Color.BLACK)
        }
    }

    if (chore == null) {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("EEEE, MMMM dd", Locale.getDefault())
        binding.textDueDate.text = dateFormat.format(calendar.time)
    }

    val repeatOptions = arrayOf("Never", "Daily", "Weekly", "Monthly", "Yearly")
    val adapter = ArrayAdapter(context, R.layout.simple_spinner_item, repeatOptions)
    adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
    binding.spinnerRepeats.adapter = adapter

    // Pre-fill if editing
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
                val dateFormat = SimpleDateFormat("EEEE, MMMM dd", Locale.getDefault())
                binding.textDueDate.text = dateFormat.format(calendar.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // CHANGED: Single selection dialog instead of multi-select
    binding.textAssignedPerson.setOnClickListener {
        showSingleHousemateSelectionDialog(
            context,
            availableHousemates,
            selectedAssignee
        ) { selected ->
            selectedAssignee = selected
            binding.textAssignedPerson.text = selected
            binding.textAssignedPerson.setTextColor(Color.BLACK)
        }
    }

    binding.buttonCancel.setOnClickListener {
        bottomSheet.dismiss()
    }

    binding.buttonSave.setOnClickListener {
        val title = binding.editTextChoreTitle.text.toString().trim()
        val dueDate = binding.textDueDate.text.toString()
        val repeats = binding.spinnerRepeats.selectedItem.toString()

        when {
            title.isEmpty() -> {
                Toast.makeText(context, "Please enter a chore title", Toast.LENGTH_SHORT).show()
            }
            selectedAssignee == null -> {
                Toast.makeText(context, "Please assign to a person", Toast.LENGTH_SHORT).show()
            }
            dueDate.isBlank() -> {
                Toast.makeText(context, "Please select a due date", Toast.LENGTH_SHORT).show()
            }
            else -> {
                // CHANGED: Single assignee in list
                val savedChore = ChoreUI(
                    id = chore?.id ?: UUID.randomUUID().toString(),
                    title = title,
                    dueDate = dueDate,
                    frequency = repeats,
                    assignedToNames = listOf(selectedAssignee!!),
                    isCompleted = chore?.isCompleted ?: false
                )

                onSave(savedChore)
                val action = if (chore == null) "Created" else "Updated"
                Toast.makeText(context, "$action: $title", Toast.LENGTH_SHORT).show()
                bottomSheet.dismiss()
            }
        }
    }

    bottomSheet.window?.setBackgroundDrawableResource(R.color.transparent)
    bottomSheet.setContentView(binding.root)
    bottomSheet.show()
}

// COMMENTED OUT: Multi-selection chip functions
/*
private fun addChipToGroup(
    binding: LayoutBottomChoreDetailBinding,
    assignee: String,
    selectedAssignees: MutableList<String>,
    context: Context
) {
    val themedContext = ContextThemeWrapper(context, com.google.android.material.R.style.Theme_MaterialComponents)
    val chip = Chip(themedContext).apply {
        text = assignee
        isCloseIconVisible = true
        setOnCloseIconClickListener {
            selectedAssignees.remove(assignee)
            binding.chipGroupAssigned.removeView(this)
        }
    }
    binding.chipGroupAssigned.addView(chip)
}

private fun showHousemateSelectionDialog(
    context: Context,
    availableHousemates: List<String>,
    currentSelected: List<String>,
    onSelected: (List<String>) -> Unit
) {
    val selectedItems = currentSelected.toMutableList()
    val checkedItems = BooleanArray(availableHousemates.size) { index ->
        availableHousemates[index] in currentSelected
    }

    AlertDialog.Builder(context)
        .setTitle("Assign to Housemates")
        .setMultiChoiceItems(
            availableHousemates.toTypedArray(),
            checkedItems
        ) { _, which, isChecked ->
            if (isChecked) {
                if (availableHousemates[which] !in selectedItems) {
                    selectedItems.add(availableHousemates[which])
                }
            } else {
                selectedItems.remove(availableHousemates[which])
            }
        }
        .setPositiveButton("OK") { _, _ ->
            onSelected(selectedItems)
        }
        .setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        .show()
}
*/

// NEW: Single selection dialog
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