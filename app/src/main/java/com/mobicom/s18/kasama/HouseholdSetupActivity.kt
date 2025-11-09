package com.mobicom.s18.kasama

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mobicom.s18.kasama.databinding.LayoutHouseholdSetupBinding

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

        viewBinding.buttonJoin.setOnClickListener {
            val joinHouseholdIntent = Intent(this, JoinActivity::class.java)
            startActivity(joinHouseholdIntent)
        }
    }
}