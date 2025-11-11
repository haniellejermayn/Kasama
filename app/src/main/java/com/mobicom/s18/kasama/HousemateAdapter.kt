package com.mobicom.s18.kasama

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mobicom.s18.kasama.databinding.LayoutItemHousemateBinding
import com.mobicom.s18.kasama.models.HousemateUI

class HousemateAdapter(private var housemates: List<HousemateUI>) : RecyclerView.Adapter<HousemateViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HousemateViewHolder {
        val binding = LayoutItemHousemateBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HousemateViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HousemateViewHolder, position: Int) {
        holder.bindData(housemates[position])
    }

    override fun getItemCount(): Int = housemates.size

    fun setHousemates(newHousemates: List<HousemateUI>) {
        housemates = newHousemates
        notifyDataSetChanged()
    }
}