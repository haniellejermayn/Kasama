package com.mobdeve.s18.group10.group10_mco2

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mobdeve.s18.group10.group10_mco2.databinding.LayoutSignupPageBinding

class SignupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = LayoutSignupPageBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}