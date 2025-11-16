package com.mobicom.s18.kasama

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.mobicom.s18.kasama.databinding.LayoutSettingsBinding
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: LayoutSettingsBinding
    private var currentHouseholdId: String? = null
    private var currentUserId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = LayoutSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set Views
        binding.buttonSaveChanges.isEnabled = false
        binding.buttonLeave.text = "LEAVE HOUSEHOLD"
        binding.buttonSaveChanges.visibility = View.GONE

        loadUserData()
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

                val currHousehold = app.householdRepository.getHouseholdById(householdId.toString()).getOrNull()
                binding.textHhname.setText(currHousehold?.name ?: "Name Unavailable")

                currHousehold?.createdBy?.let { createdById ->
                    val createdByUser = app.userRepository.getUserById(createdById).getOrNull()
                    binding.textCreatedby.text = "Created by: " + createdByUser?.displayName ?: "Unknown"
                } ?: run {
                    binding.textCreatedby.text = "Unknown"
                }

                if (userId == currHousehold?.createdBy) {
                    binding.buttonLeave.text = "DELETE HOUSEHOLD"
                    binding.buttonSaveChanges.isEnabled = true
                    binding.buttonSaveChanges.visibility = View.VISIBLE

                    handleOwnUpdate()
                } else {
                    binding.textHhname.isEnabled = false
                    handleMemberUpdate()
                }
            }
        }
    }

    private fun handleOwnUpdate() {
        lifecycleScope.launch {
            val app = application as KasamaApplication
            val currentUser = app.firebaseAuth.currentUser
            if (currentUser == null) return@launch

            val userId = currentUser.uid
            currentUserId = userId

            val userResult = app.userRepository.getUserById(userId)
            if (userResult.isSuccess.not()) return@launch

            val user = userResult.getOrNull()
            val householdId = user?.householdId
            currentHouseholdId = householdId
            if (currentHouseholdId == null) return@launch

            val household = app.householdRepository
                .getHouseholdById(currentHouseholdId!!)
                .getOrNull()
            if (household == null) return@launch

            binding.buttonSaveChanges.setOnClickListener {
                val newName = binding.textHhname.text.toString().trim()
                if (newName == household.name) {
                    Toast.makeText(this@SettingsActivity, "No changes made.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                lifecycleScope.launch {
                    val updateResult = app.householdRepository.updateHousehold(
                        householdId!!,
                        newName,
                        household.createdBy
                    )
                    if (updateResult.isSuccess) {
                        val dashboardIntent =
                            Intent(this@SettingsActivity, DashboardActivity::class.java)
                        dashboardIntent.flags =
                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(dashboardIntent)
                        Toast.makeText(this@SettingsActivity, "Household name updated.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@SettingsActivity, "Failed to update household.", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            binding.buttonLeave.setOnClickListener {
                // Check if there are other members besides the creator
                val otherMembers = household.memberIds.filter { it != userId }
                if (otherMembers.isNotEmpty()) {
                    AlertDialog.Builder(this@SettingsActivity)
                        .setTitle("Cannot delete household")
                        .setMessage("You cannot delete this household while other members are still present.")
                        .setPositiveButton("OK", null)
                        .show()
                    return@setOnClickListener
                }

                // If no other members, proceed with deletion
                AlertDialog.Builder(this@SettingsActivity)
                    .setTitle("Delete Household?")
                    .setMessage(
                        "Deleting this household will remove everything — " +
                                "chores, notes, and all members. This cannot be undone."
                    )
                    .setPositiveButton("Delete") { _, _ ->
                        lifecycleScope.launch {
                            val deleteResult = app.householdRepository.deleteHousehold(currentHouseholdId!!)
                            if (deleteResult.isSuccess) {
                                Toast.makeText(this@SettingsActivity, "Household deleted.", Toast.LENGTH_SHORT).show()

                                val dashboardIntent =
                                    Intent(this@SettingsActivity, DashboardActivity::class.java)
                                dashboardIntent.flags =
                                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(dashboardIntent)
                                finish()
                            } else {
                                Toast.makeText(this@SettingsActivity, "Failed to delete household.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }

    private fun handleMemberUpdate() {
        lifecycleScope.launch {
            val app = application as KasamaApplication
            val currentUser = app.firebaseAuth.currentUser
            if (currentUser == null) return@launch

            val userId = currentUser.uid
            currentUserId = userId

            val userResult = app.userRepository.getUserById(userId)
            if (!userResult.isSuccess) return@launch

            val user = userResult.getOrNull()
            val householdId = user?.householdId
            currentHouseholdId = householdId

            if (currentHouseholdId == null) return@launch

            val household = app.householdRepository
                .getHouseholdById(currentHouseholdId!!)
                .getOrNull()

            if (household == null) return@launch

            binding.buttonLeave.setOnClickListener {
                AlertDialog.Builder(this@SettingsActivity)
                    .setTitle("Leave Household?")
                    .setMessage(
                        "This cannot be undone."
                    )
                    .setPositiveButton("Leave") { _, _ ->
                        lifecycleScope.launch {

                            val leaveResult =
                                app.householdRepository.leaveHousehold(currentHouseholdId!!, userId)

                            if (leaveResult.isSuccess) {
                                Toast.makeText(this@SettingsActivity, "You left the household.", Toast.LENGTH_SHORT).show()

                                val dashboardIntent =
                                    Intent(this@SettingsActivity, DashboardActivity::class.java)
                                dashboardIntent.flags =
                                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(dashboardIntent)

                                finish()
                            } else {
                                Toast.makeText(this@SettingsActivity, "Failed to leave household.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }
}