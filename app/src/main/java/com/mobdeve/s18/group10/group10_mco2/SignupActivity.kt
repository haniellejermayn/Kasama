package com.mobdeve.s18.group10.group10_mco2

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import com.mobdeve.s18.group10.group10_mco2.databinding.LayoutSignupPageBinding

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