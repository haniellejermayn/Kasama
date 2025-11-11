package com.mobicom.s18.kasama

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.mobicom.s18.kasama.models.NoteUI

class NoteViewHolder(itemView: View) : ViewHolder(itemView) {
    private val titleNote: TextView = itemView.findViewById(R.id.text_note)

    fun bindNoteData(note: NoteUI) {
        titleNote.text = note.title
    }
}