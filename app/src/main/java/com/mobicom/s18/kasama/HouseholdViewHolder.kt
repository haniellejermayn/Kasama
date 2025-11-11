package com.mobicom.s18.kasama

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.mobicom.s18.kasama.data.local.entities.Household

class HouseholdViewHolder(itemView: View): ViewHolder(itemView) {
    private val name: TextView = itemView.findViewById(R.id.text_household_name)

    fun bindHouseholdData(house: Household) {
        name.text = house.name
    }
}