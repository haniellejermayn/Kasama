package com.mobdeve.s18.group10.group10_mco2

import android.view.View
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.mobdeve.s18.group10.group10_mco2.databinding.LayoutChorePageBinding
import com.mobdeve.s18.group10.group10_mco2.databinding.LayoutItemChoreBinding
import androidx.core.graphics.toColorInt

class ChoreViewHolder(private val binding: LayoutItemChoreBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bindData(chore: Chore, adapter: ChoreAdapter) {
        val context = binding.root.context

        // Set title and due date
        binding.choreItemName.text = chore.title
        binding.choreItemDueDate.text = chore.dueDate

        if (chore.isCompleted) {
            /*binding.choreItemName.paintFlags =
                binding.choreItemName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG*/
            binding.choreItemStrikethrough.visibility = View.VISIBLE
            binding.choreItemName.setTextColor("#B2133C3D".toColorInt())
            binding.choreItemDueDate.visibility = View.GONE
            binding.buttonChoreItem.setImageResource(R.drawable.checkmark_true)
        } else {
            /*binding.choreItemName.paintFlags =
                binding.choreItemName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()*/
            binding.choreItemStrikethrough.visibility = View.GONE
            binding.choreItemName.setTextColor("#000000".toColorInt())
            binding.choreItemDueDate.visibility = View.VISIBLE
            binding.buttonChoreItem.setImageResource(R.drawable.checkmark)
        }

        binding.buttonChoreItem.setOnClickListener {
            chore.isCompleted = !chore.isCompleted
            adapter.notifyItemChanged(bindingAdapterPosition)
        }
    }
}