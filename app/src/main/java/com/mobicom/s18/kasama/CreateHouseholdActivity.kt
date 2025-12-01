package com.mobicom.s18.kasama

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.mobicom.s18.kasama.databinding.LayoutCreateHouseholdBinding
import com.mobicom.s18.kasama.data.repository.AuthRepository
import com.mobicom.s18.kasama.data.repository.HouseholdRepository
import com.mobicom.s18.kasama.utils.LoadingUtils.hideLoading
import com.mobicom.s18.kasama.utils.LoadingUtils.showLoading
import kotlinx.coroutines.launch

class CreateHouseholdActivity : AppCompatActivity() {
    private lateinit var viewBinding: LayoutCreateHouseholdBinding
    private lateinit var authRepository: AuthRepository
    private lateinit var householdRepository: HouseholdRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = LayoutCreateHouseholdBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        val app = application as KasamaApplication
        authRepository = app.authRepository
        householdRepository = app.householdRepository

        viewBinding.cancelBtn.setOnClickListener {
            finish()
        }

        viewBinding.createBtn.setOnClickListener {
            handleCreateHousehold()
        }
    }

    private fun handleCreateHousehold() {
        val householdName = viewBinding.householdNameEtv.text.toString().trim()

        if (householdName.isEmpty()) {
            Toast.makeText(this, "Please enter a household name", Toast.LENGTH_SHORT).show()
            return
        }
        if (householdName.length < 3) {
            Toast.makeText(this, "Household name must be at least 3 characters", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUser = authRepository.getCurrentUser()
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        viewBinding.createBtn.isEnabled = false
        showLoading("Creating household...")

        lifecycleScope.launch {
            val result = householdRepository.createHousehold(
                name = householdName,
                createdBy = currentUser.uid
            )

            result.onSuccess { household ->
                hideLoading()
                Toast.makeText(
                    this@CreateHouseholdActivity,
                    "Household created!",
                    Toast.LENGTH_SHORT
                ).show()

                // pass household data to InviteActivity
                val inviteIntent = Intent(this@CreateHouseholdActivity, InviteActivity::class.java)
                inviteIntent.putExtra("HOUSEHOLD_ID", household.id)
                inviteIntent.putExtra("HOUSEHOLD_NAME", household.name)
                inviteIntent.putExtra("INVITE_CODE", household.inviteCode)
                startActivity(inviteIntent)
                finish()
            }

            result.onFailure { exception ->
                hideLoading()
                Toast.makeText(
                    this@CreateHouseholdActivity,
                    "Failed to create household: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
                viewBinding.createBtn.isEnabled = true
            }
        }
    }
}