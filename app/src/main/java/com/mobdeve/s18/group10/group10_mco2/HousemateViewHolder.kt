package com.mobdeve.s18.group10.group10_mco2

import android.view.View
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import com.mobdeve.s18.group10.group10_mco2.databinding.LayoutItemHousemateBinding

class HousemateViewHolder(private val binding: LayoutItemHousemateBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bindData(housemate: Housemate, adapter: HousemateAdapter) {
        val context = binding.root.context

        // Set title and due date
        binding.housemateName.text = housemate.name
        binding.choresRemaining.text = "${housemate.choresRemaining.toString()} chores remaining this week"
        // Add binding pfp later
    }
}