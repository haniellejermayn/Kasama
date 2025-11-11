package com.mobicom.s18.kasama

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.mobicom.s18.kasama.models.HouseholdUI

class HouseholdViewHolder(itemView: View) : ViewHolder(itemView) {
    private val name: TextView = itemView.findViewById(R.id.text_household_name)

    fun bindHouseholdData(household: HouseholdUI) {
        name.text = household.name
    }
}