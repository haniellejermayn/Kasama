package com.mobicom.s18.kasama

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.mobicom.s18.kasama.databinding.LayoutProfileSetupBinding
import com.mobicom.s18.kasama.data.repository.UserRepository
import com.mobicom.s18.kasama.data.repository.AuthRepository
import kotlinx.coroutines.launch
import java.util.Calendar


class ProfileSetupActivity : AppCompatActivity() {
    private lateinit var viewBinding: LayoutProfileSetupBinding
    private lateinit var userRepository: UserRepository
    private lateinit var authRepository: AuthRepository
    private var selectedBirthdate: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = LayoutProfileSetupBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        val app = application as KasamaApplication
        userRepository = app.userRepository
        authRepository = app.authRepository

        val currentUser = authRepository.getCurrentUser()
        lifecycleScope.launch {
            currentUser?.let {
                val result = userRepository.getUserById(it.uid)
                result.onSuccess { firebaseUser ->
                    viewBinding.textHelloMsg.text = "Hello, ${firebaseUser.displayName}!"
                }
            }
        }

        viewBinding.buttonConfirm.setOnClickListener {
            handleProfileSetup()
        }

        viewBinding.imagebtnAddImage.setOnClickListener {
            // TODO: file upload feature
            Toast.makeText(this, "[TEMPORARY] Must be able to file upload...", Toast.LENGTH_SHORT).show()
        }

        viewBinding.edittextBirthdate.setOnClickListener {
            showDatePicker()
        }
    }

    private fun handleProfileSetup() {
        val phoneNumber = viewBinding.edittextPhoneNum.text.toString().trim()

        // validation
        if (phoneNumber.isNotEmpty() && phoneNumber.length < 10) {
            Toast.makeText(this, "Please enter a valid phone number", Toast.LENGTH_SHORT).show()
            return
        }

        viewBinding.buttonConfirm.isEnabled = false

        val currentUser = authRepository.getCurrentUser()
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val result = userRepository.updateUserProfile(
                userId = currentUser.uid,
                phoneNumber = phoneNumber.ifEmpty { null },
                birthdate = selectedBirthdate,
                profilePictureUrl = null // TODO: Handle image upload
            )

            result.onSuccess {
                Toast.makeText(this@ProfileSetupActivity, "Profile updated!", Toast.LENGTH_SHORT).show()

                val householdSetupIntent = Intent(this@ProfileSetupActivity, HouseholdSetupActivity::class.java)
                startActivity(householdSetupIntent)
                finish()
            }

            result.onFailure { exception ->
                Toast.makeText(
                    this@ProfileSetupActivity,
                    "Update failed: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
                viewBinding.buttonConfirm.isEnabled = true
            }
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePicker = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val formattedDate = "${selectedMonth + 1}/$selectedDay/$selectedYear"
                viewBinding.edittextBirthdate.setText(formattedDate)

                // store as timestamp
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(selectedYear, selectedMonth, selectedDay)
                selectedBirthdate = selectedCalendar.timeInMillis
            },
            year,
            month,
            day
        )

        datePicker.datePicker.maxDate = System.currentTimeMillis()
        datePicker.show()
    }
}