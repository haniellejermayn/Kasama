package com.mobicom.s18.kasama

import android.graphics.Color
import android.os.Bundle
import android.view.View
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

// TODO: Show overdue chores
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
                }
            }
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
        binding.textOptionToday.setOnClickListener {
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
    }

    private fun updateDisplayedChores(filter: String) {
        currentFilter = filter
        val filteredSections = viewModel.filterChoresByFrequencyGrouped(filter)
        choreSectionAdapter.setSections(filteredSections)
        viewModel.calculateProgressForFilter(filter)

        binding.textChoreDate.text = when (filter) {
            "today" -> "Today"
            "this week" -> "This Week"
            "this month" -> "This Month"
            "all" -> "All Chores"
            else -> ""
        }
    }

    private fun highlightTab(activeTextView: TextView) {
        val tabs = listOf(
            binding.textOptionToday to binding.bottomBorderToday,
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
    }
}