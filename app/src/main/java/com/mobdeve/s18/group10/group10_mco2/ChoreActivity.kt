package com.mobdeve.s18.group10.group10_mco2

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.mobdeve.s18.group10.group10_mco2.databinding.LayoutChorePageBinding
import java.text.SimpleDateFormat
import java.util.*

class ChoreActivity : AppCompatActivity() {

    private lateinit var binding: LayoutChorePageBinding
    private lateinit var choreAdapter: ChoreAdapter

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH)

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = LayoutChorePageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        choreAdapter = ChoreAdapter(choreListSample)
        binding.choreRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.choreRecyclerView.adapter = choreAdapter

        // Default to "Daily"
        val dailyFiltered = filterChoresByType("daily", choreListSample)
        choreAdapter.setChores(dailyFiltered)
        highlightTab(binding.textOptionDailyChore)

        setupFilterTabs()
    }

    private fun setupFilterTabs() {
        binding.textOptionDailyChore.setOnClickListener {
            binding.textChoreDate.text = "Today"
            val progressPercent = (completedDailyChores * 100 / totalDailyChores)
            binding.progressBar.progress = progressPercent
            binding.progressCount.text = "$completedDailyChores / $totalDailyChores"
            choreAdapter.setChores(filterChoresByType("daily", choreListSample))
            highlightTab(binding.textOptionDailyChore)
        }

        binding.textOptionWeeklyChore.setOnClickListener {
            binding.textChoreDate.text = "This Week"
            val progressPercent = (completedWeeklyChores * 100 / totalWeeklyChores)
            binding.progressBar.progress = progressPercent
            binding.progressCount.text = "$completedWeeklyChores / $totalWeeklyChores"
            choreAdapter.setChores(filterChoresByType("weekly", choreListSample))
            highlightTab(binding.textOptionWeeklyChore)
        }

        binding.textOptionMonthlyChore.setOnClickListener {
            binding.textChoreDate.text = "This Month"
            val progressPercent = (completedMonthlyChores * 100 / totalMonthlyChores)
            binding.progressBar.progress = progressPercent
            binding.progressCount.text = "$completedMonthlyChores / $totalMonthlyChores"
            choreAdapter.setChores(filterChoresByType("monthly", choreListSample))
            highlightTab(binding.textOptionMonthlyChore)
        }
    }

    /**
     * Filters chores by time range (daily, weekly, monthly).
     * Uses Calendar instead of java.time for API 24 support.
     */
    fun filterChoresByType(type: String, choreList: List<Chore>): List<Chore> {
        val todayCal = Calendar.getInstance()
        val today = todayCal.time

        return choreList.filter { chore ->
            try {
                val dueDate = dateFormat.parse(chore.dueDate) ?: return@filter false
                val dueCal = Calendar.getInstance().apply { time = dueDate }

                when (type.lowercase()) {
                    "daily" -> isSameDay(todayCal, dueCal)
                    "weekly" -> isSameWeek(todayCal, dueCal)
                    "monthly" -> isSameMonth(todayCal, dueCal)
                    else -> false
                }
            } catch (e: Exception) {
                false
            }
        }
    }

    private fun isSameDay(c1: Calendar, c2: Calendar): Boolean {
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isSameWeek(c1: Calendar, c2: Calendar): Boolean {
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                c1.get(Calendar.WEEK_OF_YEAR) == c2.get(Calendar.WEEK_OF_YEAR)
    }

    private fun isSameMonth(c1: Calendar, c2: Calendar): Boolean {
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH)
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
