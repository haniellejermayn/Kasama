package com.mobicom.s18.kasama

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.mobicom.s18.kasama.models.NoteUI

class NoteViewHolder(itemView: View) : ViewHolder(itemView) {
    private val titleNote: TextView = itemView.findViewById(R.id.text_note)
    private val authorNote: TextView = itemView.findViewById(R.id.text_note_author)
    private val syncIndicator: ImageView = itemView.findViewById(R.id.sync_indicator_note)

    fun bindNoteData(note: NoteUI) {
        titleNote.text = note.title

        // Show author name
        val authorText = if (note.createdBy.isNotEmpty()) {
            "By ${note.createdBy}"
        } else {
            ""
        }
        authorNote.text = authorText
        authorNote.visibility = if (authorText.isNotEmpty()) View.VISIBLE else View.GONE

        // Show sync indicator
        syncIndicator.visibility = if (!note.isSynced) View.VISIBLE else View.GONE
    }
}