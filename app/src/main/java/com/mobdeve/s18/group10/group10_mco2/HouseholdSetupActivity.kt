package com.mobdeve.s18.group10.group10_mco2

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import com.mobdeve.s18.group10.group10_mco2.databinding.LayoutHouseholdSetupBinding

class HouseholdSetupActivity : AppCompatActivity() {
    private lateinit var viewBinding: LayoutHouseholdSetupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = LayoutHouseholdSetupBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        viewBinding.buttonCreate.setOnClickListener {
            val createHouseholdIntent = Intent(this, CreateHouseholdActivity::class.java)
            startActivity(createHouseholdIntent)
        }
    }
}