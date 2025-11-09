package com.mobicom.s18.kasama

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mobicom.s18.kasama.databinding.LayoutSignupPageBinding

class SignupActivity : AppCompatActivity() {

    private lateinit var viewBinding: LayoutSignupPageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = LayoutSignupPageBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        viewBinding.textSubLogin.setOnClickListener {
            val loginIntent = Intent(this, LoginActivity::class.java)
            startActivity(loginIntent)
            finish()
        }

        viewBinding.buttonSignup.setOnClickListener {
            val profileSetupIntent = Intent(this, ProfileSetupActivity::class.java)
            startActivity(profileSetupIntent)
        }
    }
}