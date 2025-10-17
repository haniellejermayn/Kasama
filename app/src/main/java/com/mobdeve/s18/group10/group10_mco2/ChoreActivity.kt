package com.mobdeve.s18.group10.group10_mco2

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.mobdeve.s18.group10.group10_mco2.databinding.ActivityChoreBinding
import com.mobdeve.s18.group10.group10_mco2.databinding.LayoutStarterPageBinding
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.cardview.widget.CardView
import android.graphics.Color
import android.os.Build
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.google.android.material.card.MaterialCardView
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ChoreActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChoreBinding
    private lateinit var choreAdapter: ChoreAdapter
    val choreListSample = listOf(
        Chore("Clean Bathroom", "Oct 17, 2025", "Weekly", "Hanielle", false),
        Chore("Change Bed Sheets", "Oct 18, 2025", "Monthly", "Hanielle", false),
        Chore("Wash Dishes", "Oct 18, 2025", "Never", "Hanielle", false),
        Chore("Take Out Trash", "Oct 20, 2025", "Daily", "Hanielle", false),
    )
    val totalDailyChores = 4
    val completedDailyChores = 1

    val totalWeeklyChores = 12
    val completedWeeklyChores = 8

    val totalMonthlyChores = 24
    val completedMonthlyChores = 21

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityChoreBinding.inflate(layoutInflater)
        setContentView(binding.root)

        choreAdapter = ChoreAdapter(choreListSample)
        binding.choreRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.choreRecyclerView.adapter = choreAdapter

        val dailyFiltered = filterChoresByType("daily", choreListSample)
        choreAdapter.setChores(dailyFiltered)
        highlightTab(binding.textOptionDailyChore)

        setupFilterTabs()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupFilterTabs() {
        binding.textOptionDailyChore.setOnClickListener {
            binding.textChoreDate.setText("Today")
            val progressPercent = (completedDailyChores * 100 / totalDailyChores)
            binding.progressBar.progress = progressPercent
            binding.progressCount.text = "$completedDailyChores / $totalDailyChores"
            val filtered = filterChoresByType("daily", choreListSample)
            choreAdapter.setChores(filtered)
            highlightTab(binding.textOptionDailyChore)
        }

        binding.textOptionWeeklyChore.setOnClickListener {
            binding.textChoreDate.setText("This Week")
            val progressPercent = (completedWeeklyChores * 100 / totalWeeklyChores)
            binding.progressBar.progress = progressPercent
            binding.progressCount.text = "$completedWeeklyChores / $totalWeeklyChores"
            val filtered = filterChoresByType("weekly", choreListSample)
            choreAdapter.setChores(filtered)
            highlightTab(binding.textOptionWeeklyChore)
        }

        binding.textOptionMonthlyChore.setOnClickListener {
            binding.textChoreDate.setText("This Month")
            val progressPercent = (completedMonthlyChores * 100 / totalMonthlyChores)
            binding.progressBar.progress = progressPercent
            binding.progressCount.text = "$completedMonthlyChores / $totalMonthlyChores"
            val filtered = filterChoresByType("monthly", choreListSample)
            choreAdapter.setChores(filtered)
            highlightTab(binding.textOptionMonthlyChore)
        }

    }
    @RequiresApi(Build.VERSION_CODES.O)
    fun filterChoresByType(type: String, choreList: List<Chore>): List<Chore> {
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")

        return choreList.filter { chore ->
            val due = LocalDate.parse(chore.dueDate, formatter)

            when (type.lowercase()) {
                "daily" -> due == today
                "weekly" -> {
                    val startOfWeek = today.with(java.time.DayOfWeek.MONDAY)
                    val endOfWeek = today.with(java.time.DayOfWeek.SUNDAY)
                    !due.isBefore(startOfWeek) && !due.isAfter(endOfWeek)
                }
                "monthly" -> due.month == today.month && due.year == today.year
                else -> false
            }
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