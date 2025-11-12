package com.mobicom.s18.kasama

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.mobicom.s18.kasama.databinding.LayoutItemChoreBinding
import com.mobicom.s18.kasama.models.ChoreUI

class ChoreAdapter(private var choreData: List<ChoreUI>) : RecyclerView.Adapter<ChoreViewHolder>() {

    private var onChoreClickListener: ((ChoreUI) -> Unit)? = null
    private var onChoreCompletedListener: ((ChoreUI) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChoreViewHolder {
        val binding = LayoutItemChoreBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChoreViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChoreViewHolder, position: Int) {
        val currentChore = choreData[position]
        holder.bindData(currentChore, this, onChoreClickListener, onChoreCompletedListener)
    }

    override fun getItemCount(): Int = choreData.size

    fun setChores(newChores: List<ChoreUI>) {
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize() = choreData.size
            override fun getNewListSize() = newChores.size

            override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean {
                return choreData[oldPos].id == newChores[newPos].id
            }

            override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean {
                return choreData[oldPos] == newChores[newPos]
            }
        }

        val diffResult = DiffUtil.calculateDiff(diffCallback)
        choreData = newChores
        diffResult.dispatchUpdatesTo(this)
    }

    fun setOnChoreClickListener(listener: ((ChoreUI) -> Unit)?) {
        onChoreClickListener = listener
    }

    fun setOnChoreCompletedListener(listener: ((ChoreUI) -> Unit)?) {
        onChoreCompletedListener = listener
    }
}