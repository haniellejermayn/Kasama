package com.mobicom.s18.kasama

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mobicom.s18.kasama.databinding.LayoutItemChoreBinding

class ChoreAdapter(private var choreData: List<Chore>) : RecyclerView.Adapter<ChoreViewHolder>() {

    private var onChoreClickListener: ((Chore) -> Unit)? = null
    private var onChoreCompletedListener: (() -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChoreViewHolder {
        val binding = LayoutItemChoreBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChoreViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChoreViewHolder, position: Int) {
        val currentChore = choreData[position]
        holder.bindData(currentChore, this, onChoreClickListener, onChoreCompletedListener)
    }

    override fun getItemCount(): Int {
        return choreData.size
    }

    fun setChores(newChores: List<Chore>) {
        choreData = newChores
        notifyDataSetChanged()
    }

    fun setOnChoreClickListener(listener: (Chore) -> Unit) {
        onChoreClickListener = listener
    }

    fun setOnChoreCompletedListener(listener: () -> Unit) {
        onChoreCompletedListener = listener
    }
}