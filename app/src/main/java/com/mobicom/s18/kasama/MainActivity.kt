package com.mobicom.s18.kasama

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.mobicom.s18.kasama.databinding.LayoutStarterPageBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var viewBinding: LayoutStarterPageBinding
    private lateinit var authRepository: com.mobicom.s18.kasama.data.repository.AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewBinding = LayoutStarterPageBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        authRepository = (application as KasamaApplication).authRepository

        // check if already logged in
        if (authRepository.isLoggedIn()) {
            checkUserHousehold()
            return
        }

        viewBinding.buttonSignUp.setOnClickListener {
            val signupIntent = Intent(this, SignupActivity::class.java)
            startActivity(signupIntent)
        }

        viewBinding.buttonLogIn.setOnClickListener {
            val loginIntent = Intent(this, LoginActivity::class.java)
            startActivity(loginIntent)
        }
    }

    private fun checkUserHousehold() {
        val currentUser = authRepository.getCurrentUser() ?: return

        lifecycleScope.launch {
            val userDao = (application as KasamaApplication).database.userDao()
            val user = userDao.getUserByIdOnce(currentUser.uid)

            val intent = if (user?.householdId != null) {
                Intent(this@MainActivity, DashboardActivity::class.java)
            } else {
                Intent(this@MainActivity, HouseholdSetupActivity::class.java)
            }

            startActivity(intent)
            finish()
        }
    }
}