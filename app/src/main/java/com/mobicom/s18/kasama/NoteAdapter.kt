package com.mobicom.s18.kasama

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
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
            val context = holder.itemView.context
            val app = context.applicationContext as KasamaApplication
            val currentUser = app.firebaseAuth.currentUser

            if (currentUser != null && context is LifecycleOwner) {
                context.lifecycleScope.launch {
                    val userResult = app.userRepository.getUserById(currentUser.uid)
                    val householdId = userResult.getOrNull()?.householdId
                    val currentUserPfp = userResult.getOrNull()?.profilePictureUrl

                    if (householdId != null) {
                        showNoteBottomSheet(
                            context = context,
                            householdId = householdId,
                            currentUserId = currentUser.uid,
                            note = notes[position],
                            profilePictureUrl = currentUserPfp,
                            onSave = {
                                // Note will be updated via Flow
                            }
                        )
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int = notes.size
}