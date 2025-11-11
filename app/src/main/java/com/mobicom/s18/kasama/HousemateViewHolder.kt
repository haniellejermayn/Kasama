package com.mobicom.s18.kasama

import androidx.recyclerview.widget.RecyclerView
import com.mobicom.s18.kasama.databinding.LayoutItemHousemateBinding
import com.mobicom.s18.kasama.models.HousemateUI

class HousemateViewHolder(private val binding: LayoutItemHousemateBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bindData(housemate: HousemateUI) {
        binding.housemateName.text = housemate.name
        binding.choresRemaining.text = "${housemate.choresRemaining} chores remaining this week"
        // TODO: Load profile picture using Glide/Coil
        // Glide.with(binding.root.context).load(housemate.profilePictureUrl).into(binding.profileImage)
    }
}