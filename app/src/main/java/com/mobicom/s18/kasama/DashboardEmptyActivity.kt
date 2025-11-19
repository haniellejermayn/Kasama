package com.mobicom.s18.kasama

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.mobicom.s18.kasama.databinding.LayoutDashboardEmptyBinding

class DashboardEmptyActivity : AppCompatActivity() {
    private lateinit var binding: LayoutDashboardEmptyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = LayoutDashboardEmptyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonHome2.setOnClickListener {
            val menu = Intent(this, MenuActivity::class.java)
            startActivity(menu)
        }
    }
}