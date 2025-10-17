package com.mobdeve.s18.group10.group10_mco2

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import com.mobdeve.s18.group10.group10_mco2.databinding.LayoutLoginPageBinding

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
    }
}