package com.mobicom.s18.kasama

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.mobicom.s18.kasama.models.NoteUI

class NoteViewHolder(itemView: View) : ViewHolder(itemView) {
    private val titleNote: TextView = itemView.findViewById(R.id.text_note)
    private val contentNote: TextView = itemView.findViewById(R.id.text_note_content)
    private val authorNote: TextView = itemView.findViewById(R.id.text_note_author)
    private val profileImageNote: ImageView = itemView.findViewById(R.id.image_note_profile_image)
    private val dateNote: TextView = itemView.findViewById(R.id.text_note_date)
    private val syncIndicator: ImageView = itemView.findViewById(R.id.sync_indicator_note)

    fun bindNoteData(note: NoteUI) {
        titleNote.text = note.title
        contentNote.text = note.content
        dateNote.text = note.createdAt
        dateNote.visibility = if (note.createdAt.isNotEmpty()) View.VISIBLE else View.GONE

        val authorText = note.createdBy.ifEmpty {
            ""
        }
        authorNote.text = authorText
        authorNote.visibility = if (authorText.isNotEmpty()) View.VISIBLE else View.GONE

        if (note.profilePictureUrl != null) {
            Glide.with(itemView.context)
                .load(note.profilePictureUrl)
                .circleCrop()
                .placeholder(R.drawable.kasama_profile_default)
                .error(R.drawable.kasama_profile_default)
                .into(profileImageNote)
        } else {
            // Set default placeholder if no profile picture
            profileImageNote.setImageResource(R.drawable.kasama_profile_default)
        }

        syncIndicator.visibility = if (!note.isSynced) View.VISIBLE else View.GONE
    }
}