package com.mobicom.s18.kasama

import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.mobicom.s18.kasama.databinding.LayoutItemHousemateBinding
import com.mobicom.s18.kasama.models.HousemateUI

class HousemateViewHolder(private val binding: LayoutItemHousemateBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bindData(housemate: HousemateUI) {
        binding.housemateName.text = housemate.name
        binding.choresRemaining.text = "${housemate.choresRemaining} chores remaining this week"

        // Load profile picture using Glide
        if (housemate.profilePictureUrl != null) {
            Glide.with(binding.root.context)
                .load(housemate.profilePictureUrl)
                .circleCrop()
                .placeholder(R.drawable.kasama_profile_default)
                .error(R.drawable.kasama_profile_default)
                .into(binding.profileImage)
        } else {
            // Set default placeholder if no profile picture
            binding.profileImage.setImageResource(R.drawable.kasama_profile_default)
        }
    }
}