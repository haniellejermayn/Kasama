package com.mobicom.s18.kasama

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mobicom.s18.kasama.databinding.LayoutItemChoreSectionBinding
import com.mobicom.s18.kasama.models.ChoreUI
import com.mobicom.s18.kasama.viewmodels.ChoreViewModel

class ChoreSectionAdapter(
    private var sections: List<ChoreViewModel.ChoreSection>
) : RecyclerView.Adapter<ChoreSectionViewHolder>() {

    private var onChoreClickListener: ((ChoreUI) -> Unit)? = null
    private var onChoreCompletedListener: ((ChoreUI) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChoreSectionViewHolder {
        val binding = LayoutItemChoreSectionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChoreSectionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChoreSectionViewHolder, position: Int) {
        holder.bind(sections[position], onChoreClickListener, onChoreCompletedListener)
    }

    override fun getItemCount(): Int = sections.size

    fun setSections(newSections: List<ChoreViewModel.ChoreSection>) {
        sections = newSections
        notifyDataSetChanged()
    }

    fun setOnChoreClickListener(listener: (ChoreUI) -> Unit) {
        onChoreClickListener = listener
    }

    fun setOnChoreCompletedListener(listener: (ChoreUI) -> Unit) {
        onChoreCompletedListener = listener
    }
}

class ChoreSectionViewHolder(
    private val binding: LayoutItemChoreSectionBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(
        section: ChoreViewModel.ChoreSection,
        onChoreClickListener: ((ChoreUI) -> Unit)?,
        onChoreCompletedListener: ((ChoreUI) -> Unit)?
    ) {
        binding.sectionUserName.text = section.userName
        binding.sectionChoreCount.text = "${section.chores.size} chore${if (section.chores.size != 1) "s" else ""}"

        binding.sectionUserName.setTextColor(binding.root.context.getColor(android.R.color.black))
        binding.sectionChoreCount.setTextColor(binding.root.context.getColor(android.R.color.darker_gray))

        // highlight current user's section
        if (section.isCurrentUser) {
            binding.sectionUserName.setTextColor(binding.root.context.getColor(R.color.primary_color))
        }

        // highlight overdue section
        if (section.isOverdue) {
            binding.sectionUserName.setTextColor(binding.root.context.getColor(android.R.color.holo_red_dark))
            binding.sectionChoreCount.setTextColor(binding.root.context.getColor(android.R.color.holo_red_dark))
        }

        // setup nested RecyclerView for chores
        val choreAdapter = ChoreAdapter(section.chores)
        choreAdapter.setOnChoreClickListener(onChoreClickListener)
        choreAdapter.setOnChoreCompletedListener(onChoreCompletedListener)
        choreAdapter.setIsOverdueSection(section.isOverdue)

        binding.sectionChoreRecyclerView.layoutManager = LinearLayoutManager(binding.root.context)
        binding.sectionChoreRecyclerView.adapter = choreAdapter
    }
}