package com.mobicom.s18.kasama

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mobicom.s18.kasama.databinding.LayoutNotesPageBinding

class NoteActivity : AppCompatActivity() {
    private lateinit var viewBinding: LayoutNotesPageBinding
    private lateinit var rv: RecyclerView
    private val noteList: ArrayList<Note> = NoteDataGenerator.generateNotes()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewBinding = LayoutNotesPageBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        this.rv = viewBinding.rvNotes
        rv.layoutManager = GridLayoutManager(this, 3)
        this.rv.adapter = NoteAdapter(noteList)
    }
}