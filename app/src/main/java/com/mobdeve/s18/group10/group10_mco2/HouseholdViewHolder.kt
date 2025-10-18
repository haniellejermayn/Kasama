package com.mobdeve.s18.group10.group10_mco2

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView.ViewHolder

class HouseholdViewHolder(itemView: View): ViewHolder(itemView) {
    private val name: TextView = itemView.findViewById(R.id.text_household_name)

    fun bindHouseholdData(house: Household) {
        name.text = house.name
    }
}