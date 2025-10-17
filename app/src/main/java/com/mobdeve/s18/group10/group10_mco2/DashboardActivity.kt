package com.mobdeve.s18.group10.group10_mco2

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.mobdeve.s18.group10.group10_mco2.databinding.LayoutDashboardPageBinding

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: LayoutDashboardPageBinding
    private lateinit var choreAdapter: ChoreAdapter
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

        // Show chores tab by default
        showTab(Tab.CHORES, animate = false)
    }

    private fun setupRecyclerViews() {
        choreAdapter = ChoreAdapter(choreListSample)
        binding.dashboardChoreRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.dashboardChoreRecyclerView.adapter = choreAdapter

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

    private fun showTab(tab: Tab, animate: Boolean = true) {
        currentTab = tab

        // Update underlines
        updateTabUnderlines(tab, animate)

        // Update content visibility with fade animation
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
            // Animate underlines
            animateUnderline(binding.bottomBorderChores, tab == Tab.CHORES)
            animateUnderline(binding.bottomBorderNotes, tab == Tab.NOTES)
            animateUnderline(binding.bottomBorderHousemates, tab == Tab.HOUSEMATES)
        } else {
            // Set visibility immediately
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
                binding.dashboardHousemateRecyclerView.isVisible = false
            }
            Tab.NOTES -> {
                binding.buttonNewNote.isVisible = true
                binding.buttonViewAllNotes.isVisible = true

                binding.buttonNewChore.isVisible = false
                binding.buttonViewAllChores.isVisible = false
                binding.dashboardChoreRecyclerView.isVisible = false
                binding.dashboardHousemateRecyclerView.isVisible = false
            }
            Tab.HOUSEMATES -> {
                binding.dashboardHousemateRecyclerView.isVisible = true

                binding.buttonNewChore.isVisible = false
                binding.buttonViewAllChores.isVisible = false
                binding.dashboardChoreRecyclerView.isVisible = false
                binding.buttonNewNote.isVisible = false
                binding.buttonViewAllNotes.isVisible = false
            }
        }
    }

    private fun fadeOutCurrentContent(onComplete: () -> Unit) {
        val viewsToFade = listOf(
            binding.buttonNewChore,
            binding.buttonNewNote,
            binding.buttonViewAllChores,
            binding.buttonViewAllNotes,
            binding.dashboardChoreRecyclerView,
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

        // Call onComplete after animation
        binding.root.postDelayed(onComplete, 150)
    }

    private fun fadeInNewContent() {
        val viewsToFade = listOf(
            binding.buttonNewChore,
            binding.buttonNewNote,
            binding.buttonViewAllChores,
            binding.buttonViewAllNotes,
            binding.dashboardChoreRecyclerView,
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