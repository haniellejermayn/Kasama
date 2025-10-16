package com.mobdeve.s18.group10.group10_mco2

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mobdeve.s18.group10.group10_mco2.databinding.LayoutLoginPageBinding

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = LayoutLoginPageBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}