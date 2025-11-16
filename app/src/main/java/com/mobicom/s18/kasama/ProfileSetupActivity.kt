package com.mobicom.s18.kasama

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.mobicom.s18.kasama.databinding.LayoutProfileSetupBinding
import kotlinx.coroutines.launch
import java.util.Calendar

class ProfileSetupActivity : AppCompatActivity() {
    private lateinit var binding: LayoutProfileSetupBinding
    private var selectedBirthdate: Long? = null
    private var selectedImageUri: Uri? = null

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            Glide.with(this)
                .load(it)
                .circleCrop()
                .into(binding.imageProfileIcon)  // ✅ Fixed: Update profile icon, not button
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LayoutProfileSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val app = application as KasamaApplication

        lifecycleScope.launch {
            app.firebaseAuth.currentUser?.let { currentUser ->
                val result = app.userRepository.getUserById(currentUser.uid)
                result.onSuccess { firebaseUser ->
                    binding.textHelloMsg.text = "Hello, ${firebaseUser.displayName}!"
                }
            }
        }

        binding.buttonConfirm.setOnClickListener {
            handleProfileSetup()
        }

        binding.imagebtnAddImage.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        binding.edittextBirthdate.setOnClickListener {
            showDatePicker()
        }

        binding.edittextName.visibility = View.GONE
        val mode = intent.getStringExtra("MODE") ?: "default"
        if (mode == "edit") {
            setUpEditProfile()
        }
    }

    private fun handleProfileSetup() {
        val phoneNumber = binding.edittextPhoneNum.text.toString().trim()

        if (phoneNumber.isNotEmpty() && phoneNumber.length < 10) {
            Toast.makeText(this, "Please enter a valid phone number", Toast.LENGTH_SHORT).show()
            return
        }

        binding.buttonConfirm.isEnabled = false

        val app = application as KasamaApplication
        val currentUser = app.firebaseAuth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            binding.buttonConfirm.isEnabled = true
            return
        }

        lifecycleScope.launch {
            try {
                var profilePictureUrl: String? = null
                if (selectedImageUri != null) {
                    Toast.makeText(
                        this@ProfileSetupActivity,
                        "Uploading image...",
                        Toast.LENGTH_SHORT
                    ).show()

                    val uploadResult = app.storageRepository.uploadProfilePicture(
                        currentUser.uid,
                        selectedImageUri!!
                    )

                    if (uploadResult.isSuccess) {
                        profilePictureUrl = uploadResult.getOrNull()
                    } else {
                        throw uploadResult.exceptionOrNull() ?: Exception("Upload failed")
                    }
                }

                val result = app.userRepository.updateUserProfile(
                    userId = currentUser.uid,
                    phoneNumber = phoneNumber.ifEmpty { null },
                    birthdate = selectedBirthdate,
                    profilePictureUrl = profilePictureUrl
                )

                result.onSuccess {
                    Toast.makeText(
                        this@ProfileSetupActivity,
                        "Profile updated!",
                        Toast.LENGTH_SHORT
                    ).show()

                    val householdSetupIntent = Intent(
                        this@ProfileSetupActivity,
                        HouseholdSetupActivity::class.java
                    )
                    startActivity(householdSetupIntent)
                    finish()
                }

                result.onFailure { exception ->
                    throw exception
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@ProfileSetupActivity,
                    "Update failed: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                binding.buttonConfirm.isEnabled = true
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
                binding.edittextBirthdate.setText(formattedDate)

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

    private fun setUpEditProfile() {
        binding.textHelloMsg.visibility = View.GONE
        binding.textSub.visibility = View.GONE
        binding.edittextName.visibility = View.VISIBLE

        lifecycleScope.launch {
            val app = application as KasamaApplication
            val currentUser = app.firebaseAuth.currentUser
            if (currentUser == null) {
                return@launch
            }

            val userId = currentUser.uid
            val userResult = app.userRepository.getUserById(userId)
            if (userResult.isSuccess) {
                val user = userResult.getOrNull()

                binding.edittextName.setText(user?.displayName)
                binding.edittextPhoneNum.setText(user?.phoneNumber)

                if (user?.profilePictureUrl != null) {
                    Glide.with(this@ProfileSetupActivity)
                        .load(user.profilePictureUrl)
                        .circleCrop()
                        .placeholder(R.drawable.kasama_profile_default)
                        .error(R.drawable.kasama_profile_default)
                        .into(binding.imageProfileIcon)
                } else {
                    binding.imageProfileIcon.setImageResource(R.drawable.kasama_profile_default)
                }

                val birthdateMillis = user?.birthdate ?: 0L
                if (birthdateMillis > 0) {
                    val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                    val date = java.util.Date(birthdateMillis)
                    binding.edittextBirthdate.setText(sdf.format(date))
                } else {
                    binding.edittextBirthdate.setText("")
                }

                binding.buttonConfirm.setOnClickListener {
                    val newName = binding.edittextName.text.toString().trim()
                    val newNum = binding.edittextPhoneNum.text.toString().trim()
                    val newBirthdate = selectedBirthdate ?: 0L

                    val oldName = user?.displayName?.trim() ?: ""
                    val oldNum = user?.phoneNumber ?: ""
                    val oldBirthdate = user?.birthdate ?: 0L


                    if ((newBirthdate == 0L || newBirthdate == oldBirthdate) && newName == oldName && newNum == oldNum) {
                        Toast.makeText(this@ProfileSetupActivity, "No changes made.", Toast.LENGTH_SHORT).show()
                    } else {
                        handleEditProfile()
                    }
                }
            }
        }
    }

    private fun handleEditProfile() {
        val phoneNumber = binding.edittextPhoneNum.text.toString().trim()

        if (phoneNumber.isNotEmpty() && phoneNumber.length < 10) {
            Toast.makeText(this, "Please enter a valid phone number", Toast.LENGTH_SHORT).show()
            return
        }

        binding.buttonConfirm.isEnabled = false

        val app = application as KasamaApplication
        val currentUser = app.firebaseAuth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            binding.buttonConfirm.isEnabled = true
            return
        }

        lifecycleScope.launch {
            try {
                var profilePictureUrl: String? = null
                if (selectedImageUri != null) {
                    binding.progressBarUpload.visibility = View.VISIBLE
                    Toast.makeText(
                        this@ProfileSetupActivity,
                        "Uploading image...",
                        Toast.LENGTH_SHORT
                    ).show()

                    val uploadResult = app.storageRepository.uploadProfilePicture(
                        currentUser.uid,
                        selectedImageUri!!
                    )

                    if (uploadResult.isSuccess) {
                        profilePictureUrl = uploadResult.getOrNull()
                        binding.progressBarUpload.visibility = View.GONE
                    } else {
                        binding.progressBarUpload.visibility = View.GONE
                        throw uploadResult.exceptionOrNull() ?: Exception("Upload failed")
                    }
                }

                val newName = binding.edittextName.text.toString()

                val result = app.userRepository.updateUserProfile(
                    name = newName,
                    userId = currentUser.uid,
                    phoneNumber = phoneNumber.ifEmpty { null },
                    birthdate = selectedBirthdate,
                    profilePictureUrl = profilePictureUrl
                )

                result.onSuccess {
                    Toast.makeText(
                        this@ProfileSetupActivity,
                        "Profile updated!",
                        Toast.LENGTH_SHORT
                    ).show()

                    val dashboard = Intent(
                        this@ProfileSetupActivity,
                        DashboardActivity::class.java
                    )
                    startActivity(dashboard)
                    finish()
                }

                result.onFailure { exception ->
                    throw exception
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@ProfileSetupActivity,
                    "Update failed: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                binding.buttonConfirm.isEnabled = true
            }
        }
    }
}