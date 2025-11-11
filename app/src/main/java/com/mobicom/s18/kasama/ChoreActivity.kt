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
import com.mobicom.s18.kasama.models.ChoreUI
import com.mobicom.s18.kasama.utils.showChoreBottomSheet
import com.mobicom.s18.kasama.viewmodels.ChoreViewModel
import kotlinx.coroutines.launch

class ChoreActivity : AppCompatActivity() {

    private lateinit var binding: LayoutChorePageBinding
    private lateinit var choreAdapter: ChoreAdapter

    private val viewModel: ChoreViewModel by viewModels {
        val app = application as KasamaApplication
        ChoreViewModel.Factory(app.choreRepository, app.userRepository, app.database)
    }

    private var currentHouseholdId: String? = null
    private var currentFrequency: String = "daily"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = LayoutChorePageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        observeViewModel()
        setupFilterTabs()

        loadUserHousehold()
    }

    private fun loadUserHousehold() {
        lifecycleScope.launch {
            val app = application as KasamaApplication
            val currentUser = app.firebaseAuth.currentUser
            if (currentUser != null) {
                val userResult = app.userRepository.getUserById(currentUser.uid)
                if (userResult.isSuccess) {
                    val user = userResult.getOrNull()
                    currentHouseholdId = user?.householdId
                    currentHouseholdId?.let { householdId ->
                        viewModel.loadChoresByHousehold(householdId)
                        updateDisplayedChores("daily")
                    }
                }
            }
        }
    }

    private fun setupRecyclerView() {
        choreAdapter = ChoreAdapter(emptyList())
        binding.choreRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.choreRecyclerView.adapter = choreAdapter

        choreAdapter.setOnChoreClickListener { chore ->
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
                            currentUserId = app.firebaseAuth.currentUser?.uid ?: "",
                            chore = chore,
                            onSave = {
                                viewModel.loadChoresByHousehold(householdId)
                            }
                        )
                    }
                }
            }
        }

        choreAdapter.setOnChoreCompletedListener { chore ->
            currentHouseholdId?.let { householdId ->
                viewModel.toggleChoreCompletion(chore.id, householdId)
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.chores.collect { chores ->
                updateDisplayedChores(currentFrequency)
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
        binding.textOptionDailyChore.setOnClickListener {
            updateDisplayedChores("daily")
            highlightTab(binding.textOptionDailyChore)
        }

        binding.textOptionWeeklyChore.setOnClickListener {
            updateDisplayedChores("weekly")
            highlightTab(binding.textOptionWeeklyChore)
        }

        binding.textOptionMonthlyChore.setOnClickListener {
            updateDisplayedChores("monthly")
            highlightTab(binding.textOptionMonthlyChore)
        }
    }

    private fun updateDisplayedChores(frequency: String) {
        currentFrequency = frequency
        val filteredChores = viewModel.filterChoresByFrequency(frequency)
        choreAdapter.setChores(filteredChores)
        viewModel.calculateProgress(frequency)

        binding.textChoreDate.text = when (frequency) {
            "daily" -> "Today"
            "weekly" -> "This Week"
            "monthly" -> "This Month"
            else -> ""
        }
    }

    private fun highlightTab(activeTextView: TextView) {
        val tabs = listOf(
            binding.textOptionDailyChore to binding.bottomBorderDaily,
            binding.textOptionWeeklyChore to binding.bottomBorderWeekly,
            binding.textOptionMonthlyChore to binding.bottomBorderMonthly
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