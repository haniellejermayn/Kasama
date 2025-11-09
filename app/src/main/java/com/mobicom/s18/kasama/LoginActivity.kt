package com.mobicom.s18.kasama

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mobicom.s18.kasama.databinding.LayoutLoginPageBinding

class LoginActivity : AppCompatActivity() {
    private lateinit var viewBinding: LayoutLoginPageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = LayoutLoginPageBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        viewBinding.textSubSignup.setOnClickListener {
            val signupIntent = Intent(this, SignupActivity::class.java)
            startActivity(signupIntent)
            finish()
        }

        viewBinding.buttonLogin.setOnClickListener {
            val dashboardIntent = Intent(this, DashboardActivity::class.java)
            startActivity(dashboardIntent)
            finish()
        }
    }
}