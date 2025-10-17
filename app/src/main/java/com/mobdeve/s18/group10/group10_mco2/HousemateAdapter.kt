package com.mobdeve.s18.group10.group10_mco2

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mobdeve.s18.group10.group10_mco2.databinding.LayoutItemHousemateBinding

class HousemateAdapter(private var housemateData: List<Housemate>) : RecyclerView.Adapter<HousemateViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HousemateViewHolder {
        val binding = LayoutItemHousemateBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HousemateViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HousemateViewHolder, position: Int) {
        val currentHousemate = housemateData[position]
        holder.bindData(currentHousemate, this)
    }

    override fun getItemCount(): Int {
        return housemateData.size
    }
}