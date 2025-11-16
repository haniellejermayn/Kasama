package com.mobicom.s18.kasama

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.mobicom.s18.kasama.data.local.entities.Household
import com.mobicom.s18.kasama.data.remote.models.FirebaseHousehold
import com.mobicom.s18.kasama.databinding.LayoutMenuBinding
import com.mobicom.s18.kasama.models.HouseholdUI
import com.mobicom.s18.kasama.notifications.NotificationScheduler
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MenuActivity : AppCompatActivity() {
    private lateinit var viewBinding: LayoutMenuBinding
    private lateinit var rv_households: RecyclerView
    private lateinit var household_adapter: HouseholdAdapter
    private lateinit var household_viewholder: HouseholdViewHolder

    private var currentUserId: String? = null
    private var currentHouseholdId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = LayoutMenuBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        setupButtonListeners()
        loadUserData()
    }

    fun setupButtonListeners() {
        viewBinding.buttonEditProfile.setOnClickListener {
            val editProfileIntent = Intent(this, ProfileSetupActivity::class.java)
            startActivity(editProfileIntent)
        }

        viewBinding.buttonLogOut.setOnClickListener {
            val app = application as KasamaApplication
            app.authRepository.logOut()

            NotificationScheduler.cancelChoreReminders(this)

            val loginIntent = Intent(this, LoginActivity::class.java)
            loginIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(loginIntent)
            finish()
        }

        viewBinding.buttonJoinHousehold.setOnClickListener {
            val hhsetup = Intent(this, HouseholdSetupActivity::class.java)
            startActivity(hhsetup)
        }
    }

    private fun loadUserData() {
        lifecycleScope.launch {
            val app = application as KasamaApplication
            val currentUser = app.firebaseAuth.currentUser
            if (currentUser == null) {
                return@launch
            }

            val userId = currentUser.uid
            currentUserId = userId

            val userResult = app.userRepository.getUserById(userId)
            if (userResult.isSuccess) {
                val user = userResult.getOrNull()
                val householdId = user?.householdId
                currentHouseholdId = householdId

                // Update user info
                viewBinding.textName.text = user?.displayName ?: "User"
                val birthdateText = user?.birthdate?.let { timestamp ->
                    val sdf = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
                    sdf.format(Date(timestamp))
                } ?: "N/A"
                viewBinding.textBday.text = birthdateText ?: "N/A"
                viewBinding.textNumber.text = user?.phoneNumber ?: "N/A"

                // Load profile picture
                if (user?.profilePictureUrl != null) {
                    Glide.with(this@MenuActivity)
                        .load(user.profilePictureUrl)
                        .circleCrop()
                        .placeholder(R.drawable.kasama_profile_default)
                        .error(R.drawable.kasama_profile_default)
                        .into(viewBinding.imageProfile)
                } else {
                    viewBinding.imageProfile.setImageResource(R.drawable.kasama_profile_default)
                }

                // Get all households the user is in
                Log.d("Households", "User is in ${user?.householdIDs?.size} households: ${user?.householdIDs}")
                val allHouseholds = user?.householdIDs
                    ?.mapNotNull { hsId ->
                        val result = app.householdRepository.getHouseholdById(hsId)
                        result.getOrNull()
                    } ?: emptyList()

                val householdUIs = allHouseholds.map { hs ->
                    HouseholdUI(
                        id = hs.id,
                        name = hs.name,
                        isActive = false
                    )
                }

                rv_households = viewBinding.rvSidetabHousehold
                rv_households.layoutManager = LinearLayoutManager(this@MenuActivity)
                rv_households.adapter = HouseholdAdapter(householdUIs) { selected ->
                    lifecycleScope.launch {
                        if (selected.id == user?.householdId) {
                            Toast.makeText(this@MenuActivity, "Household already selected.", Toast.LENGTH_SHORT).show()
                        } else {
                            val result = app.householdRepository.updateCurrentHousehold(
                                householdId = selected.id,
                                userId = currentUserId.toString()
                            )

                            if (result.isSuccess) {
                                val updatedHousehold = result.getOrNull()
                                Toast.makeText(
                                    this@MenuActivity,
                                    "Switched household!",
                                    Toast.LENGTH_SHORT
                                ).show()

                                val dashboardIntent =
                                    Intent(this@MenuActivity, DashboardActivity::class.java)
                                dashboardIntent.flags =
                                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(dashboardIntent)

                                finish()
                            } else {
                                Toast.makeText(
                                    this@MenuActivity,
                                    "Failed to switch household",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }

                Log.d("Households", "User is in ${allHouseholds.size} households: $allHouseholds")
            }
        }
    }
}