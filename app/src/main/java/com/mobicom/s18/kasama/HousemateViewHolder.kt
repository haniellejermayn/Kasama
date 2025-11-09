package com.mobicom.s18.kasama

import androidx.recyclerview.widget.RecyclerView
import com.mobicom.s18.kasama.databinding.LayoutItemHousemateBinding

class HousemateViewHolder(private val binding: LayoutItemHousemateBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bindData(housemate: Housemate, adapter: HousemateAdapter) {
        val context = binding.root.context

        // Set title and due date
        binding.housemateName.text = housemate.name
        binding.choresRemaining.text = "${housemate.choresRemaining.toString()} chores remaining this week"
        // Add binding pfp later
    }
}