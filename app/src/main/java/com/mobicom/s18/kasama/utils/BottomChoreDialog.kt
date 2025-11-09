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
import androidx.appcompat.view.ContextThemeWrapper
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.mobicom.s18.kasama.Chore
import com.mobicom.s18.kasama.databinding.LayoutBottomChoreDetailBinding
import java.text.SimpleDateFormat
import java.util.*

fun showChoreBottomSheet(
    context: Context,
    availableHousemates: List<String>,
    chore: Chore? = null,
    onSave: (Chore) -> Unit = {}
) {
    val bottomSheet = BottomSheetDialog(context)
    bottomSheet.behavior.isFitToContents = true
    val binding = LayoutBottomChoreDetailBinding.inflate(LayoutInflater.from(context))

    val selectedAssignees = mutableListOf<String>()

    // Pre-fill if editing
    chore?.let {
        binding.editTextChoreTitle.setText(it.title)
        binding.textDueDate.text = it.dueDate

        it.assignedToList.forEach { assignee ->
            selectedAssignees.add(assignee)
            addChipToGroup(binding, assignee, selectedAssignees, context)
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

    binding.chipAddAssignee.setOnClickListener {
        showHousemateSelectionDialog(context, availableHousemates, selectedAssignees) { selected ->
            binding.chipGroupAssigned.removeAllViews()
            selectedAssignees.clear()
            selectedAssignees.addAll(selected)

            selected.forEach { housemate ->
                addChipToGroup(binding, housemate, selectedAssignees, context)
            }
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
            selectedAssignees.isEmpty() -> {
                Toast.makeText(context, "Please assign to at least one person", Toast.LENGTH_SHORT).show()
            }
            dueDate.isBlank() -> {
                Toast.makeText(context, "Please select a due date", Toast.LENGTH_SHORT).show()
            }
            else -> {
                // Create or update chore
                val savedChore = if (chore == null) {
                    Chore(
                        title = title,
                        dueDate = dueDate,
                        frequency = repeats,
                        assignedToList = selectedAssignees,
                        isCompleted = false
                    )
                } else {
                    chore.apply {
                        this.title = title
                        this.dueDate = dueDate
                        this.frequency = repeats
                        this.assignedToList = selectedAssignees
                    }
                }

                // Call the callback with the saved chore
                onSave(savedChore)

                val action = if (chore == null) "Created" else "Updated"
                Toast.makeText(context, "$action: $title", Toast.LENGTH_SHORT).show()
                bottomSheet.dismiss()
            }
        }
    }

    bottomSheet.window?.setBackgroundDrawableResource(R.color.transparent)
    bottomSheet.setContentView(binding.root)
    bottomSheet.setOnShowListener {
        val bottomSheetView =
            bottomSheet.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheetView?.setBackgroundColor(Color.TRANSPARENT)
    }
    bottomSheet.show()
}

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