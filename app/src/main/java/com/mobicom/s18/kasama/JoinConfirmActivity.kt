package com.mobicom.s18.kasama

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.mobicom.s18.kasama.databinding.LayoutJoinConfirmBinding
import com.mobicom.s18.kasama.data.repository.AuthRepository
import com.mobicom.s18.kasama.data.repository.HouseholdRepository
import kotlinx.coroutines.launch
import com.mobicom.s18.kasama.utils.LoadingUtils.showLoading
import com.mobicom.s18.kasama.utils.LoadingUtils.hideLoading

class JoinConfirmActivity : AppCompatActivity() {
    private lateinit var viewBinding: LayoutJoinConfirmBinding
    private lateinit var authRepository: AuthRepository
    private lateinit var householdRepository: HouseholdRepository

    private lateinit var householdId: String
    private lateinit var inviteCode: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = LayoutJoinConfirmBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        val app = application as KasamaApplication
        authRepository = app.authRepository
        householdRepository = app.householdRepository

        val householdName = intent.getStringExtra("HOUSEHOLD_NAME") ?: "Household"
        val createdBy = intent.getStringExtra("CREATED_BY") ?: "Unknown"
        val memberCount = intent.getIntExtra("MEMBER_COUNT", 0)
        householdId = intent.getStringExtra("HOUSEHOLD_ID") ?: ""
        inviteCode = intent.getStringExtra("INVITE_CODE") ?: ""

        viewBinding.householdNameTv.text = householdName
        viewBinding.createdByTv.text = "created by $createdBy"
        viewBinding.numMembersTv.text = "$memberCount members"

        viewBinding.backBtn.setOnClickListener {
            finish()
        }

        viewBinding.confirmBtn.setOnClickListener {
            handleJoinConfirm()
        }
    }

    private fun handleJoinConfirm() {
        val currentUser = authRepository.getCurrentUser()
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        viewBinding.confirmBtn.isEnabled = false
        showLoading("Joining household...")

        lifecycleScope.launch {
            val result = householdRepository.joinHousehold(
                inviteCode = inviteCode,
                userId = currentUser.uid
            )

            result.onSuccess {
                hideLoading()
                Toast.makeText(
                    this@JoinConfirmActivity,
                    "Successfully joined household!",
                    Toast.LENGTH_SHORT
                ).show()

                val dashboardIntent = Intent(this@JoinConfirmActivity, DashboardActivity::class.java)
                // Clear back stack so user can't go back to join pages
                dashboardIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(dashboardIntent)
                finish()
            }

            result.onFailure { exception ->
                hideLoading()
                Toast.makeText(
                    this@JoinConfirmActivity,
                    "Failed to join: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
                viewBinding.confirmBtn.isEnabled = true
            }
        }
    }
}