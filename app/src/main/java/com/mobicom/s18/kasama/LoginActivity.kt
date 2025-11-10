package com.mobicom.s18.kasama

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.mobicom.s18.kasama.databinding.LayoutLoginPageBinding
import com.mobicom.s18.kasama.data.repository.AuthRepository
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var viewBinding: LayoutLoginPageBinding
    private lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = LayoutLoginPageBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        authRepository = (application as KasamaApplication).authRepository

        viewBinding.textSubSignup.setOnClickListener {
            val signupIntent = Intent(this, SignupActivity::class.java)
            startActivity(signupIntent)
            finish()
        }

        viewBinding.buttonLogin.setOnClickListener {
            handleLogin()
        }
    }

    private fun handleLogin() {
        val email = viewBinding.edittextEmail.text.toString().trim()
        val password = viewBinding.edittextPassword.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this@LoginActivity, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        viewBinding.buttonLogin.isEnabled = false

        // launch coroutine to handle login
        lifecycleScope.launch {
            val result = authRepository.logIn(email, password)

            result.onSuccess {
                Toast.makeText(this@LoginActivity, "Login successful!", Toast.LENGTH_SHORT).show()

                // check if user already has household
                val currUser = authRepository.getCurrentUser() ?: throw Exception("User not found")
                val userDao = (application as KasamaApplication).database.userDao()
                val user = userDao.getUserByIdOnce(currUser.uid)

                if (user?.householdId != null) {
                    startActivity(Intent(this@LoginActivity, DashboardActivity::class.java))
                } else {
                    startActivity(Intent(this@LoginActivity, HouseholdSetupActivity::class.java))
                }
                finish()
            }

            result.onFailure { exception ->
                Toast.makeText(
                    this@LoginActivity,
                    "Login failed: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
                viewBinding.buttonLogin.isEnabled = true
            }
        }
    }
}