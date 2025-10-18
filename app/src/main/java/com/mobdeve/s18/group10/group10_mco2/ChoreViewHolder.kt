package com.mobdeve.s18.group10.group10_mco2

import android.view.View
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import com.mobdeve.s18.group10.group10_mco2.databinding.LayoutItemChoreBinding

class ChoreViewHolder(private val binding: LayoutItemChoreBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bindData(chore: Chore, adapter: ChoreAdapter, onChoreClickListener: ((Chore) -> Unit)?, onChoreCompletedListener: (() -> Unit)? = null) {
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
            chore.isCompleted = !chore.isCompleted
            adapter.notifyItemChanged(bindingAdapterPosition)
            onChoreCompletedListener?.invoke()
        }

        binding.root.setOnClickListener {
            onChoreClickListener?.invoke(chore)
        }
    }
}