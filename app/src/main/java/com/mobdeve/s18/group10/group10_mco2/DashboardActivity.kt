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
        Chore("Clean Bathroom", "Oct 17, 2025", "Weekly", "Hanielle", false),
        Chore("Change Bed Sheets", "Oct 18, 2025", "Monthly", "Hanielle", false),
        Chore("Wash Dishes", "Oct 18, 2025", "Never", "Hanielle", false),
        Chore("Take Out Trash", "Oct 20, 2025", "Daily", "Hanielle", false),
    )

    val noteListSample = arrayListOf(
        Note("No staying up past midnight", "Don't stay up. Please."),
        Note("There's a spider in the room", "Please deal with it."),
        Note("Stay Hydrated!", "Please deal with it."),
        Note("Turn off lights not in use", "Please deal with it."),
        Note("Replace wallpaper", "Please deal with it."),
        Note("Throw away tangerine peels", "Please deal with it.")
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

        // TODO: we need to create a function that actually computes this
        binding.circularProgress.progress = 64

        showTab(Tab.CHORES, animate = false)
    }

    private fun setupRecyclerViews() {
        choreAdapter = ChoreAdapter(choreListSample)
        binding.dashboardChoreRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.dashboardChoreRecyclerView.adapter = choreAdapter

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
            // TODO: setup
        }

        binding.buttonViewAllChores.setOnClickListener {
            // TODO: setup
        }

        binding.buttonNewNote.setOnClickListener {
            // TODO: setup
        }

        binding.buttonViewAllNotes.setOnClickListener {
            // TODO: setup
        }

        binding.buttonInviteMember.setOnClickListener {
            val inviteIntent = Intent(this, InviteActivity::class.java)
            startActivity(inviteIntent)
        }
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