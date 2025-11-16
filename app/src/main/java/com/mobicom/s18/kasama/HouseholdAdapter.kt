package com.mobicom.s18.kasama

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mobicom.s18.kasama.databinding.LayoutItemHouseholdBinding
import com.mobicom.s18.kasama.models.HouseholdUI

class HouseholdAdapter(
    private var households: List<HouseholdUI>,
    private val onHouseholdClick: (HouseholdUI) -> Unit
) : RecyclerView.Adapter<HouseholdViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HouseholdViewHolder {
        val binding = LayoutItemHouseholdBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HouseholdViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HouseholdViewHolder, position: Int) {
        val item = households[position]
        holder.bindHouseholdData(item)

        holder.itemView.setOnClickListener {
            onHouseholdClick(item)
        }
    }

    override fun getItemCount() = households.size
}
