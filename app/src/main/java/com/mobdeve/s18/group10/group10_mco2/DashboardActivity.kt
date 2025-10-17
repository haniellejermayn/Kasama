package com.mobdeve.s18.group10.group10_mco2

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.mobdeve.s18.group10.group10_mco2.databinding.LayoutChorePageBinding
import com.mobdeve.s18.group10.group10_mco2.databinding.LayoutDashboardPageBinding

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: LayoutDashboardPageBinding
    private lateinit var choreAdapter: ChoreAdapter
    private lateinit var housemateAdapter: HousemateAdapter
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

        choreAdapter = ChoreAdapter(choreListSample)
        binding.dashboardChoreRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.dashboardChoreRecyclerView.adapter = choreAdapter

        housemateAdapter = HousemateAdapter(housemateListSample)
        binding.dashboardHousemateRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.dashboardHousemateRecyclerView.adapter = housemateAdapter
    }
}