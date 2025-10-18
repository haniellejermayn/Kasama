package com.mobdeve.s18.group10.group10_mco2

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.mobdeve.s18.group10.group10_mco2.databinding.LayoutDashboardPageBinding
import com.mobdeve.s18.group10.group10_mco2.utils.showChoreBottomSheet
import com.mobdeve.s18.group10.group10_mco2.utils.showNoteBottomSheet

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: LayoutDashboardPageBinding
    private lateinit var choreAdapter: ChoreAdapter
    private lateinit var noteAdapter: NoteAdapter
    private lateinit var housemateAdapter: HousemateAdapter

    private var currentTab = Tab.CHORES

    enum class Tab {
        CHORES, NOTES, HOUSEMATES
    }

    val choreListSample = listOf(
        Chore("Clean Bathroom", "Oct 17, 2025", "Weekly", listOf("Hanielle"), false),
        Chore("Change Bed Sheets", "Oct 18, 2025", "Monthly", listOf("Hanielle", "Hep"), false),
        Chore("Wash Dishes", "Oct 18, 2025", "Never", listOf("Kelsey"), false),
        Chore("Take Out Trash", "Oct 20, 2025", "Daily", listOf("Hanielle", "Hep", "Kelsey"), false),
    )

    val noteListSample = arrayListOf(
        Note("No staying up past midnight", "Don't stay up. Please."),
        Note("There's a spider in the room", "Please deal with it."),
        Note("Stay Hydrated!", "Drink water regularly"),
        Note("Turn off lights not in use", "Save electricity"),
        Note("Replace wallpaper", "Living room needs new look"),
        Note("Throw away tangerine peels", "In the kitchen")
    )

    val housemateListSample = listOf(
        Housemate("Hanielle", 2, R.drawable.kasama_profile_default),
        Housemate("Hep", 0, R.drawable.kasama_profile_default),
        Housemate("Kelsey", 4, R.drawable.kasama_profile_default),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = LayoutDashboardPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerViews()
        setupTabListeners()
        setupButtonListeners()
        updateChoreProgress()

        showTab(Tab.CHORES, animate = false)
    }

    private fun setupRecyclerViews() {
        choreAdapter = ChoreAdapter(choreListSample)
        binding.dashboardChoreRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.dashboardChoreRecyclerView.adapter = choreAdapter

        choreAdapter.setOnChoreClickListener { chore ->
            val housemateNames = housemateListSample.map { it.name }
            showChoreBottomSheet(
                context = this,
                availableHousemates = housemateNames,
                chore = chore
            )
        }

        noteAdapter = NoteAdapter(noteListSample)
        binding.dashboardNotesRecyclerView.layoutManager = GridLayoutManager(this, 3)
        binding.dashboardNotesRecyclerView.adapter = noteAdapter

        housemateAdapter = HousemateAdapter(housemateListSample)
        binding.dashboardHousemateRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.dashboardHousemateRecyclerView.adapter = housemateAdapter
    }

    private fun setupTabListeners() {
        binding.textOptionChores.setOnClickListener {
            if (currentTab != Tab.CHORES) {
                showTab(Tab.CHORES)
            }
        }

        binding.textOptionNotes.setOnClickListener {
            if (currentTab != Tab.NOTES) {
                showTab(Tab.NOTES)
            }
        }

        binding.textOptionHousemates.setOnClickListener {
            if (currentTab != Tab.HOUSEMATES) {
                showTab(Tab.HOUSEMATES)
            }
        }
    }

    private fun setupButtonListeners() {
        binding.buttonNewChore.setOnClickListener {
            val housemateNames = housemateListSample.map { it.name }
            showChoreBottomSheet(
                context = this,
                availableHousemates = housemateNames,
                chore = null
            )
        }

        binding.buttonViewAllChores.setOnClickListener {
            val allChoresIntent = Intent(this, ChoreActivity::class.java)
            startActivity(allChoresIntent)
        }

        binding.buttonViewAllNotes.setOnClickListener {
            val allNotesIntent = Intent(this, NoteActivity::class.java)
            startActivity(allNotesIntent)
        }

        binding.buttonNewNote.setOnClickListener {
            showNoteBottomSheet(this) { title, content ->
                val newNote = Note(title, content)
                noteListSample.add(newNote)
                noteAdapter.notifyItemInserted(noteListSample.size - 1)
            }
        }

        binding.buttonInviteMember.setOnClickListener {
            val inviteIntent = Intent(this, InviteActivity::class.java)
            startActivity(inviteIntent)
        }
    }

    private fun updateChoreProgress() {
        val completedChores = choreListSample.count { it.isCompleted }
        val totalChores = choreListSample.size
        val progressPercentage = if (totalChores > 0) {
            (completedChores.toFloat() / totalChores.toFloat() * 100).toInt()
        } else {
            0
        }

        binding.circularProgress.progress = progressPercentage

        binding.percentageDashboardProgress.text = "$progressPercentage%"

        val message = when {
            progressPercentage == 100 -> "You've completed all your chores!"
            progressPercentage >= 75 -> "Almost there! Keep it up!"
            progressPercentage >= 50 -> "You're halfway through your chores!"
            progressPercentage > 0 -> "You have completed $progressPercentage% of your chores!"
            else -> "Let's get started on those chores!"
        }
        binding.dashboardPercentText.text = message
    }

    private fun showTab(tab: Tab, animate: Boolean = true) {
        currentTab = tab
        updateTabUnderlines(tab, animate)

        if (animate) {
            fadeOutCurrentContent {
                updateContentVisibility(tab)
                fadeInNewContent()
            }
        } else {
            updateContentVisibility(tab)
        }
    }

    private fun updateTabUnderlines(tab: Tab, animate: Boolean) {
        if (animate) {
            animateUnderline(binding.bottomBorderChores, tab == Tab.CHORES)
            animateUnderline(binding.bottomBorderNotes, tab == Tab.NOTES)
            animateUnderline(binding.bottomBorderHousemates, tab == Tab.HOUSEMATES)
        } else {
            binding.bottomBorderChores.visibility = if (tab == Tab.CHORES) View.VISIBLE else View.INVISIBLE
            binding.bottomBorderNotes.visibility = if (tab == Tab.NOTES) View.VISIBLE else View.INVISIBLE
            binding.bottomBorderHousemates.visibility = if (tab == Tab.HOUSEMATES) View.VISIBLE else View.INVISIBLE
        }
    }

    private fun animateUnderline(view: View, show: Boolean) {
        val alpha = if (show) 1f else 0f
        ObjectAnimator.ofFloat(view, "alpha", view.alpha, alpha).apply {
            duration = 200
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
        view.visibility = if (show) View.VISIBLE else View.INVISIBLE
    }

    private fun updateContentVisibility(tab: Tab) {
        when (tab) {
            Tab.CHORES -> {
                binding.buttonNewChore.isVisible = true
                binding.buttonViewAllChores.isVisible = true
                binding.dashboardChoreRecyclerView.isVisible = true

                binding.buttonNewNote.isVisible = false
                binding.buttonViewAllNotes.isVisible = false
                binding.dashboardNotesRecyclerView.isVisible = false
                binding.buttonInviteMember.isVisible = false
                binding.dashboardHousemateRecyclerView.isVisible = false
            }
            Tab.NOTES -> {
                binding.buttonNewNote.isVisible = true
                binding.buttonViewAllNotes.isVisible = true
                binding.dashboardNotesRecyclerView.isVisible = true

                binding.buttonNewChore.isVisible = false
                binding.buttonViewAllChores.isVisible = false
                binding.dashboardChoreRecyclerView.isVisible = false
                binding.buttonInviteMember.isVisible = false
                binding.dashboardHousemateRecyclerView.isVisible = false
            }
            Tab.HOUSEMATES -> {
                binding.buttonInviteMember.isVisible = true
                binding.dashboardHousemateRecyclerView.isVisible = true

                binding.buttonNewChore.isVisible = false
                binding.buttonViewAllChores.isVisible = false
                binding.dashboardChoreRecyclerView.isVisible = false
                binding.buttonNewNote.isVisible = false
                binding.buttonViewAllNotes.isVisible = false
                binding.dashboardNotesRecyclerView.isVisible = false
            }
        }
    }

    private fun fadeOutCurrentContent(onComplete: () -> Unit) {
        val viewsToFade = listOf(
            binding.buttonNewChore,
            binding.buttonNewNote,
            binding.buttonInviteMember,
            binding.buttonViewAllChores,
            binding.buttonViewAllNotes,
            binding.dashboardChoreRecyclerView,
            binding.dashboardNotesRecyclerView,
            binding.dashboardHousemateRecyclerView
        )

        viewsToFade.forEach { view ->
            if (view.isVisible) {
                ObjectAnimator.ofFloat(view, "alpha", 1f, 0f).apply {
                    duration = 150
                    interpolator = AccelerateDecelerateInterpolator()
                    start()
                }
            }
        }

        binding.root.postDelayed(onComplete, 150)
    }

    private fun fadeInNewContent() {
        val viewsToFade = listOf(
            binding.buttonNewChore,
            binding.buttonNewNote,
            binding.buttonInviteMember,
            binding.buttonViewAllChores,
            binding.buttonViewAllNotes,
            binding.dashboardChoreRecyclerView,
            binding.dashboardNotesRecyclerView,
            binding.dashboardHousemateRecyclerView
        )

        viewsToFade.forEach { view ->
            if (view.isVisible) {
                view.alpha = 0f
                ObjectAnimator.ofFloat(view, "alpha", 0f, 1f).apply {
                    duration = 150
                    interpolator = AccelerateDecelerateInterpolator()
                    start()
                }
            }
        }
    }
}