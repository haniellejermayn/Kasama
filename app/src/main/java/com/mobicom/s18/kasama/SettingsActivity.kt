package com.mobicom.s18.kasama

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
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
        binding.textHhname.apply {
            isEnabled = false
            isFocusable = false
            isFocusableInTouchMode = false
            isCursorVisible = false
            keyListener = null
        }
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

                val originalText = currHousehold?.name ?: ""

                binding.textHhname.addTextChangedListener(object : android.text.TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        binding.buttonSaveChanges.isEnabled = s.toString() != originalText
                    }
                    override fun afterTextChanged(s: android.text.Editable?) {}
                })

                currHousehold?.createdBy?.let { createdById ->
                    val createdByUser = app.userRepository.getUserById(createdById).getOrNull()
                    binding.textCreatedby.text = "Created by: " + createdByUser?.displayName ?: "Unknown"
                } ?: run {
                    binding.textCreatedby.text = "Unknown"
                }

                val originalKeyListener = binding.textHhname.keyListener

                if (userId == currHousehold?.createdBy) {
                    binding.textHhname.apply {
                        isEnabled = true
                        isFocusable = true
                        isFocusableInTouchMode = true
                        isCursorVisible = true
                        keyListener = originalKeyListener
                    }
                    binding.buttonLeave.text = "DELETE HOUSEHOLD"
                }
            }
        }
    }
}