package com.mobicom.s18.kasama

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.mobicom.s18.kasama.databinding.LayoutChorePageBinding
import com.mobicom.s18.kasama.utils.showChoreBottomSheet
import com.mobicom.s18.kasama.viewmodels.ChoreViewModel
import kotlinx.coroutines.launch
import com.mobicom.s18.kasama.utils.LoadingUtils.showLoading
import com.mobicom.s18.kasama.utils.LoadingUtils.hideLoading

class ChoreActivity : AppCompatActivity() {

    private lateinit var binding: LayoutChorePageBinding
    private lateinit var choreSectionAdapter: ChoreSectionAdapter

    private val viewModel: ChoreViewModel by viewModels {
        val app = application as KasamaApplication
        ChoreViewModel.Factory(
            app.choreRepository,
            app.userRepository,
            app.householdRepository, 
            app.database
        )
    }

    private var currentHouseholdId: String? = null
    private var currentUserId: String? = null
    private var currentFilter: String = "today"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = LayoutChorePageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentHouseholdId = intent.getStringExtra("household_id")
        currentUserId = intent.getStringExtra("user_id")

        setupRecyclerView()
        observeViewModel()
        setupFilterTabs()

        loadHouseholdChores()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshData()
    }

    private fun loadHouseholdChores() {
        showLoading("Loading chores...")
        lifecycleScope.launch {
            val app = application as KasamaApplication

            if (currentHouseholdId == null || currentUserId == null) {
                val currentUser = app.firebaseAuth.currentUser
                if (currentUser != null) {
                    currentUserId = currentUser.uid
                    val userResult = app.userRepository.getUserById(currentUser.uid)
                    if (userResult.isSuccess) {
                        val user = userResult.getOrNull()
                        currentHouseholdId = user?.householdId
                    }
                }
            }

            currentHouseholdId?.let { householdId ->
                currentUserId?.let { userId ->
                    viewModel.loadChoresGroupedByUser(householdId, userId)
                    updateDisplayedChores("today")
                    hideLoading()
                } ?: hideLoading()
            } ?: hideLoading()
        }
    }

    private fun setupRecyclerView() {
        choreSectionAdapter = ChoreSectionAdapter(emptyList())
        binding.choreRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.choreRecyclerView.adapter = choreSectionAdapter

        choreSectionAdapter.setOnChoreClickListener { chore ->
            val householdId = currentHouseholdId
            val userId = currentUserId
            val memberCache = viewModel.householdMemberCache.value

            if (householdId == null || userId == null) {
                Toast.makeText(this, "User data not loaded yet", Toast.LENGTH_SHORT).show()
                return@setOnChoreClickListener
            }

            if (memberCache.isEmpty()) {
                Toast.makeText(this, "Loading household members...", Toast.LENGTH_SHORT).show()
                return@setOnChoreClickListener
            }

            // No loading needed - opens instantly!
            showChoreBottomSheet(
                context = this,
                memberCache = memberCache,
                householdId = householdId,
                currentUserId = userId,
                chore = chore,
                onSave = {
                    viewModel.loadChoresGroupedByUser(householdId, userId)
                }
            )
        }

        choreSectionAdapter.setOnChoreCompletedListener { chore ->
            currentHouseholdId?.let { householdId ->
                viewModel.toggleChoreCompletion(chore.id, householdId)
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.choreSections.collect { sections ->
                updateDisplayedChores(currentFilter)
            }
        }

        lifecycleScope.launch {
            viewModel.progressData.collect { (completed, total, percentage) ->
                binding.progressBar.progress = percentage
                binding.progressCount.text = "$completed / $total"
            }
        }
    }

    private fun setupFilterTabs() {
        val filterIcon = binding.filterIcon

        filterIcon.setOnClickListener {
            val popup = PopupMenu(this, filterIcon)
            popup.menuInflater.inflate(R.menu.chore_filter_menu, popup.menu)

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.filter_today -> updateDisplayedChores("today")
                    R.id.filter_overdue -> updateDisplayedChores("overdue")
                    R.id.filter_week -> updateDisplayedChores("this week")
                    R.id.filter_month -> updateDisplayedChores("this month")
                    R.id.filter_all -> updateDisplayedChores("all")
                }
                true
            }

            popup.show()
        }
    }

    private fun updateDisplayedChores(filter: String) {
        currentFilter = filter
        val filteredSections = viewModel.filterChoresByFrequencyGrouped(filter)

        if (filteredSections.isEmpty() || filteredSections.all { it.chores.isEmpty() }) {
            binding.choreRecyclerView.visibility = View.GONE
            binding.emptyStateText.visibility = View.VISIBLE

            binding.emptyStateText.text = when (filter) {
                "today" -> "No chores due today! 🎉\nCheck 'Overdue' for pending tasks."
                "overdue" -> "No overdue chores! 🎊\nYou're all caught up!"
                "this week" -> "No chores this week.\nEnjoy your free time!"
                "this month" -> "No chores this month.\nTime to relax!"
                "all" -> "No chores found.\nCreate one to get started!"
                else -> "No chores found."
            }
        } else {
            binding.choreRecyclerView.visibility = View.VISIBLE
            binding.emptyStateText.visibility = View.GONE
        }

        choreSectionAdapter.setSections(filteredSections)
        viewModel.calculateProgressForFilter(filter)

        binding.textChoreDate.text = when (filter) {
            "today" -> "Today"
            "this week" -> "This Week"
            "this month" -> "This Month"
            "overdue" -> "Overdue"
            "all" -> "All Chores"
            else -> ""
        }

        binding.currFilter.text = when (filter) {
            "today" -> "Today"
            "this week" -> "This Week"
            "this month" -> "This Month"
            "overdue" -> "Overdue"
            "all" -> "All Chores"
            else -> ""
        }
    }
}