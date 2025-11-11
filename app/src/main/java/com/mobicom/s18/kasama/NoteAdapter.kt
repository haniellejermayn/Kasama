package com.mobicom.s18.kasama

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.mobicom.s18.kasama.models.NoteUI
import com.mobicom.s18.kasama.utils.showNoteBottomSheet

class NoteAdapter(private val notes: MutableList<NoteUI>) : Adapter<NoteViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.layout_item_sticky_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bindNoteData(notes[position])

        holder.itemView.setOnClickListener {
            showNoteBottomSheet(holder.itemView.context, notes[position]) { title, content ->
                notes[position] = notes[position].copy(title = title, content = content)
                notifyItemChanged(position)
            }
        }
    }

    override fun getItemCount(): Int = notes.size
}