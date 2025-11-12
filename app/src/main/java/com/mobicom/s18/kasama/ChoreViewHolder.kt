package com.mobicom.s18.kasama

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
        onChoreCompletedListener: ((ChoreUI) -> Unit)? = null
    ) {
        binding.choreItemName.text = chore.title
        binding.choreItemDueDate.text = chore.dueDate

        // Update UI based on completion status
        if (chore.isCompleted) {
            binding.choreItemStrikethrough.visibility = View.VISIBLE
            binding.choreItemName.setTextColor("#B2133C3D".toColorInt())
            binding.choreItemDueDate.visibility = View.GONE
            binding.buttonChoreItem.setImageResource(R.drawable.checkmark_true)
        } else {
            binding.choreItemStrikethrough.visibility = View.GONE
            binding.choreItemName.setTextColor("#000000".toColorInt())
            binding.choreItemDueDate.visibility = View.VISIBLE
            binding.buttonChoreItem.setImageResource(R.drawable.checkmark)
        }

        binding.buttonChoreItem.setOnClickListener {
            onChoreCompletedListener?.invoke(chore)
        }

        binding.root.setOnClickListener {
            onChoreClickListener?.invoke(chore)
        }
    }
}