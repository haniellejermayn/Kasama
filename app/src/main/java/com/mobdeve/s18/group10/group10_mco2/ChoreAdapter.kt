package com.mobdeve.s18.group10.group10_mco2

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mobdeve.s18.group10.group10_mco2.databinding.LayoutChorePageBinding
import com.mobdeve.s18.group10.group10_mco2.databinding.LayoutItemChoreBinding

class ChoreAdapter(private var choreData: List<Chore>) : RecyclerView.Adapter<ChoreViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChoreViewHolder {
        val binding = LayoutItemChoreBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChoreViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChoreViewHolder, position: Int) {
        val currentChore = choreData[position]
        holder.bindData(currentChore, this)
    }

    override fun getItemCount(): Int {
        return choreData.size
    }

    fun setChores(newChores: List<Chore>) {
        choreData = newChores
        notifyDataSetChanged()
    }

}