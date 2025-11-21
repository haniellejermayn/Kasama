package com.mobicom.s18.kasama

import android.graphics.Color
import android.view.View
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import com.mobicom.s18.kasama.databinding.LayoutItemChoreBinding
import com.mobicom.s18.kasama.models.ChoreUI

class ChoreViewHolder(private val binding: LayoutItemChoreBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bindData(
        chore: ChoreUI,
        adapter: ChoreAdapter,
        onChoreClickListener: ((ChoreUI) -> Unit)?,
        onChoreCompletedListener: ((ChoreUI) -> Unit)? = null,
        isOverdueSection: Boolean = false
    ) {
        binding.choreItemName.text = chore.title
        binding.choreItemDueDate.text = chore.dueDate

        // Show sync indicator
        if (!chore.isSynced) {
            binding.syncIndicator.visibility = View.VISIBLE
            binding.syncIndicator.animate().rotation(360f).setDuration(1000).withEndAction {
                binding.syncIndicator.rotation = 0f
            }
        } else {
            binding.syncIndicator.visibility = View.GONE
        }

        // Update UI based on completion status
        if (chore.isCompleted) {
            // Apply native strikethrough
            binding.choreItemName.paintFlags =
                binding.choreItemName.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG

            // Optional: slightly dim the text
            binding.choreItemName.setTextColor("#B2133C3D".toColorInt())
            binding.choreItemDueDate.setTextColor("#B2666666".toColorInt())
            binding.choreItemDueDate.setTypeface(null, android.graphics.Typeface.NORMAL)
            binding.buttonChoreItem.setImageResource(R.drawable.checkmark_true)

            // TODO: disable uncompleted chores for recurring chores for now, fix the uncomplete function next time
            val isRecurring = chore.frequency != "Never" && chore.frequency.isNotBlank()
            binding.buttonChoreItem.isEnabled = !isRecurring
            binding.buttonChoreItem.alpha = if (isRecurring) 0.5f else 1.0f
        } else {
            // Remove strikethrough
            binding.choreItemName.paintFlags =
                binding.choreItemName.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()

            binding.choreItemName.setTextColor("#000000".toColorInt())
            binding.choreItemDueDate.visibility = View.VISIBLE
            binding.buttonChoreItem.setImageResource(R.drawable.checkmark)

            if (isOverdueSection || chore.isOverdue) {
                binding.choreItemDueDate.setTextColor(binding.root.context.getColor(android.R.color.holo_red_dark))
                binding.choreItemDueDate.setTypeface(null, android.graphics.Typeface.BOLD)
                binding.elementChoreEntryCard.setCardBackgroundColor("#FFCCCC".toColorInt())
            } else {
                binding.choreItemDueDate.setTextColor("#666666".toColorInt())
                binding.choreItemDueDate.setTypeface(null, android.graphics.Typeface.NORMAL)
            }
        }

        // Show frequency badge
        if (chore.frequency != "Never" && chore.frequency.isNotBlank()) {
            binding.frequencyBadge.text = chore.frequency
            binding.frequencyBadge.visibility = View.VISIBLE
        } else {
            binding.frequencyBadge.visibility = View.GONE
        }

        binding.buttonChoreItem.setOnClickListener {
            onChoreCompletedListener?.invoke(chore)
        }

        binding.root.setOnClickListener {
            onChoreClickListener?.invoke(chore)
        }
    }
}