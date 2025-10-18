package com.mobdeve.s18.group10.group10_mco2

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView.Adapter
import android.view.View
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.mobdeve.s18.group10.group10_mco2.databinding.LayoutBottomNoteDetailBinding
import kotlin.random.Random

class NoteAdapter(private val note: ArrayList<Note>): Adapter<NoteViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): NoteViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.layout_item_sticky_note, parent, false)

        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: NoteViewHolder,
        position: Int
    ) {
        holder.bindNoteData(note.get(position))

        holder.itemView.setOnClickListener {
            val bottomSheet = BottomSheetDialog(holder.itemView.context)
            val binding = LayoutBottomNoteDetailBinding.inflate(
                LayoutInflater.from(holder.itemView.context)
            )

            binding.editTextTitle.setText(note[position].title)
            binding.editTextContent.setText(note[position].content)

            binding.buttonCancel.setOnClickListener { bottomSheet.dismiss() }
            binding.buttonSave.setOnClickListener {
                Toast.makeText(holder.itemView.context, "Note saved!", Toast.LENGTH_SHORT).show()
                bottomSheet.dismiss()
            }

            bottomSheet.setContentView(binding.root)
            bottomSheet.show()
        }
    }

    override fun getItemCount(): Int {
        return note.size
    }
}