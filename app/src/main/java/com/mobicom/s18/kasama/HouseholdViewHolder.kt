package com.mobicom.s18.kasama

import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.mobicom.s18.kasama.databinding.LayoutItemHouseholdBinding
import com.mobicom.s18.kasama.models.HouseholdUI

class HouseholdViewHolder(
    private val binding: LayoutItemHouseholdBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bindHouseholdData(house: HouseholdUI) {
        binding.textHouseholdName.text = house.name
    }
}
