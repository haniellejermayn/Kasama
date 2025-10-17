package com.mobdeve.s18.group10.group10_mco2

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mobdeve.s18.group10.group10_mco2.databinding.LayoutProfileSetupBinding

class ProfileSetupActivity : AppCompatActivity() {
    private lateinit var viewBinding: LayoutProfileSetupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = LayoutProfileSetupBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        viewBinding.buttonConfirm.setOnClickListener {
            val householdSetupIntent = Intent(this, HouseholdSetupActivity::class.java)
            startActivity(householdSetupIntent)
            finish()
        }
    }
}