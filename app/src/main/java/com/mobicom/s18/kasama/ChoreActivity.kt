package com.mobicom.s18.kasama

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import android.widget.TextView
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

// in progress TODO: loading states for all activities
   // DONE activities: login, signup, createHousehold, chore, dashboard, invite, joinconfirm
// TODO: make clickable buttons more prominent
// DONE TODO: change UI of note, make it easier to read
// DONE(?) TODO: notifications
// DONE TODO: make the filter thing funnel thing
// TODO: color change (idk what) -- change the whole theme ig, but i think we can defend this if ever

class ChoreActivity : AppCompatActivity() {

    private lateinit var binding: LayoutChorePageBinding
    private lateinit var choreSectionAdapter: ChoreSectionAdapter

    private val viewModel: ChoreViewModel by viewModels {
        val app = application as KasamaApplication
        ChoreViewModel.Factory(app.choreRepository, app.userRepository, app.database)
    }

    private var currentHouseholdId: String? = null
    private var currentUserId: String? = null
    private var currentFilter: String = "today"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = LayoutChorePageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get data from intent
        currentHouseholdId = intent.getStringExtra("household_id")
        currentUserId = intent.getStringExtra("user_id")

        setupRecyclerView()
        observeViewModel()
        setupFilterTabs()

        loadHouseholdChores()
    }

    private fun loadHouseholdChores() {
        showLoading("Loading chores...")
        lifecycleScope.launch {
            val app = application as KasamaApplication

            // Get household and user IDs if not passed via intent
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
            lifecycleScope.launch {
                val app = application as KasamaApplication
                currentHouseholdId?.let { householdId ->
                    val householdResult = app.householdRepository.getHouseholdById(householdId)
                    if (householdResult.isSuccess) {
                        val household = householdResult.getOrNull()
                        val housemateNames = household?.memberIds?.mapNotNull { userId ->
                            app.userRepository.getUserById(userId).getOrNull()?.displayName
                        } ?: emptyList()

                        showChoreBottomSheet(
                            context = this@ChoreActivity,
                            availableHousemates = housemateNames,
                            householdId = householdId,
                            currentUserId = currentUserId ?: "",
                            chore = chore,
                            onSave = {
                                currentUserId?.let { userId ->
                                    viewModel.loadChoresGroupedByUser(householdId, userId)
                                }
                            }
                        )
                    }
                }
            }
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


        /*binding.textOptionToday.setOnClickListener {
            updateDisplayedChores("today")
            highlightTab(binding.textOptionToday)
        }

        binding.textOptionWeek.setOnClickListener {
            updateDisplayedChores("this week")
            highlightTab(binding.textOptionWeek)
        }

        binding.textOptionMonth.setOnClickListener {
            updateDisplayedChores("this month")
            highlightTab(binding.textOptionMonth)
        }

        binding.textOptionAll.setOnClickListener {
            updateDisplayedChores("all")
            highlightTab(binding.textOptionAll)
        }

        binding.textOptionOverdue.setOnClickListener {
            updateDisplayedChores("overdue")
            highlightTab(binding.textOptionOverdue)
        }*/
    }

    private fun updateDisplayedChores(filter: String) {
        currentFilter = filter
        val filteredSections = viewModel.filterChoresByFrequencyGrouped(filter)

        // Show/hide empty state
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

    /*private fun highlightTab(activeTextView: TextView) {
        val tabs = listOf(
            binding.textOptionToday to binding.bottomBorderToday,
            binding.textOptionOverdue to binding.bottomBorderOverdue,
            binding.textOptionWeek to binding.bottomBorderWeek,
            binding.textOptionMonth to binding.bottomBorderMonth,
            binding.textOptionAll to binding.bottomBorderAll
        )

        tabs.forEach { (textView, borderView) ->
            if (textView == activeTextView) {
                textView.setTextColor(Color.WHITE)
                borderView.visibility = View.VISIBLE
            } else {
                textView.setTextColor(Color.LTGRAY)
                borderView.visibility = View.GONE
            }
        }
    }*/
}