package com.mobdeve.s18.group10.group10_mco2

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.mobdeve.s18.group10.group10_mco2.utils.showNoteBottomSheet

class HouseholdAdapter(private val household: ArrayList<Household>): Adapter<HouseholdViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): HouseholdViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.layout_item_household, parent, false)

        return HouseholdViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: HouseholdViewHolder,
        position: Int
    ) {
        holder.bindHouseholdData(household.get(position))
    }

    override fun getItemCount(): Int {
        return household.size
    }
}