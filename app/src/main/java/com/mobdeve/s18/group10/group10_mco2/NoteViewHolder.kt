package com.mobdeve.s18.group10.group10_mco2

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView.ViewHolder

class NoteViewHolder(itemView: View): ViewHolder(itemView) {
    private val titleNote: TextView = itemView.findViewById(R.id.text_note)

    fun bindNoteData(note: Note) {
        titleNote.text = note.title
    }
}