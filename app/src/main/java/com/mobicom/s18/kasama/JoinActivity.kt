package com.mobicom.s18.kasama

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.mobicom.s18.kasama.databinding.LayoutJoinPageBinding
import com.mobicom.s18.kasama.data.repository.AuthRepository
import com.mobicom.s18.kasama.data.repository.HouseholdRepository
import com.mobicom.s18.kasama.data.repository.UserRepository
import kotlinx.coroutines.launch

class JoinActivity : AppCompatActivity() {
    private lateinit var viewBinding: LayoutJoinPageBinding
    private lateinit var authRepository: AuthRepository
    private lateinit var householdRepository: HouseholdRepository
    private lateinit var userRepository: UserRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = LayoutJoinPageBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        val app = application as KasamaApplication
        authRepository = app.authRepository
        householdRepository = app.householdRepository
        userRepository = app.userRepository

        viewBinding.cancelBtn.setOnClickListener {
            finish()
        }

        viewBinding.joinBtn.setOnClickListener {
            handleJoinHousehold()
        }
    }

    private fun handleJoinHousehold() {
        val inviteCode = viewBinding.householdNameEtv.text.toString().trim().uppercase()

        // Validation
        if (inviteCode.isEmpty()) {
            Toast.makeText(this, "Please enter an invite code", Toast.LENGTH_SHORT).show()
            return
        }

        if (inviteCode.length != 6) {
            Toast.makeText(this, "Invite code must be 6 characters", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUser = authRepository.getCurrentUser()
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        viewBinding.joinBtn.isEnabled = false

        lifecycleScope.launch {
            // First, get household info to show in confirmation
            val householdResult = householdRepository.getHouseholdByInviteCode(inviteCode)

            householdResult.onSuccess { household ->
                // Get creator's name
                val creatorResult = userRepository.getUserById(household.createdBy)

                creatorResult.onSuccess { creator ->
                    // Go to confirmation page
                    val joinConfirmIntent = Intent(this@JoinActivity, JoinConfirmActivity::class.java)
                    joinConfirmIntent.putExtra("HOUSEHOLD_ID", household.id)
                    joinConfirmIntent.putExtra("HOUSEHOLD_NAME", household.name)
                    joinConfirmIntent.putExtra("INVITE_CODE", inviteCode)
                    joinConfirmIntent.putExtra("CREATED_BY", creator.displayName)
                    joinConfirmIntent.putExtra("MEMBER_COUNT", household.memberIds.size)
                    startActivity(joinConfirmIntent)
                    finish()
                }

                creatorResult.onFailure {
                    // Still go to confirmation even if we can't get creator name
                    val joinConfirmIntent = Intent(this@JoinActivity, JoinConfirmActivity::class.java)
                    joinConfirmIntent.putExtra("HOUSEHOLD_ID", household.id)
                    joinConfirmIntent.putExtra("HOUSEHOLD_NAME", household.name)
                    joinConfirmIntent.putExtra("INVITE_CODE", inviteCode)
                    joinConfirmIntent.putExtra("CREATED_BY", "Unknown")
                    joinConfirmIntent.putExtra("MEMBER_COUNT", household.memberIds.size)
                    startActivity(joinConfirmIntent)
                    finish()
                }
            }

            householdResult.onFailure { exception ->
                Toast.makeText(
                    this@JoinActivity,
                    "Invalid invite code: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
                viewBinding.joinBtn.isEnabled = true
            }
        }
    }
}