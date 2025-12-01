package com.mobicom.s18.kasama
import com.mobicom.s18.kasama.utils.LoadingUtils.showLoading
import com.mobicom.s18.kasama.utils.LoadingUtils.hideLoading

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.mobicom.s18.kasama.data.repository.AuthRepository
import com.mobicom.s18.kasama.databinding.LayoutSignupPageBinding
import kotlinx.coroutines.launch

class SignupActivity : AppCompatActivity() {

    private lateinit var viewBinding: LayoutSignupPageBinding
    private lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = LayoutSignupPageBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        authRepository = (application as KasamaApplication).authRepository

        viewBinding.textSubLogin.setOnClickListener {
            val loginIntent = Intent(this, LoginActivity::class.java)
            startActivity(loginIntent)
            finish()
        }

        viewBinding.buttonSignup.setOnClickListener {
            handleSignup()
        }
    }

    private fun handleSignup() {
        val email = viewBinding.edittextEmail.text.toString().trim()
        val password = viewBinding.edittextPasswordCreate.text.toString()
        val confirmPassword = viewBinding.edittextPasswordConfirm.text.toString()
        val displayName = viewBinding.edittextDisplayName.text.toString().trim()

        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || displayName.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords don't match", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return
        }

        viewBinding.buttonSignup.isEnabled = false
        showLoading("Creating account...")

        // launch coroutine to handle signup
        lifecycleScope.launch {
            val result = authRepository.signUp(email, password, displayName)

            result.onSuccess {
                hideLoading()
                Toast.makeText(this@SignupActivity, "Account created successfully!", Toast.LENGTH_SHORT).show()

                val profileSetupIntent = Intent(this@SignupActivity, ProfileSetupActivity::class.java)
                startActivity(profileSetupIntent)
                finish()
            }

            result.onFailure { exception ->
                hideLoading()
                Toast.makeText(
                    this@SignupActivity,
                    "Signup failed: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
                viewBinding.buttonSignup.isEnabled = true
            }
        }

    }
}