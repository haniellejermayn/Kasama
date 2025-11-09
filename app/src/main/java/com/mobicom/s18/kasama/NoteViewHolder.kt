package com.mobicom.s18.kasama

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView.ViewHolder

class NoteViewHolder(itemView: View): ViewHolder(itemView) {
    private val titleNote: TextView = itemView.findViewById(R.id.text_note)

    fun bindNoteData(note: Note) {
        titleNote.text = note.title
    }
}