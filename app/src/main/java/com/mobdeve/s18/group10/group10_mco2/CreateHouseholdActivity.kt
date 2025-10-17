package com.mobdeve.s18.group10.group10_mco2

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mobdeve.s18.group10.group10_mco2.databinding.LayoutCreateHouseholdBinding

class CreateHouseholdActivity : AppCompatActivity() {
    private lateinit var viewBinding: LayoutCreateHouseholdBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = LayoutCreateHouseholdBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        viewBinding.cancelBtn.setOnClickListener {
            finish()
        }

        viewBinding.createBtn.setOnClickListener {
            val inviteIntent = Intent(this, InviteActivity::class.java)
            startActivity(inviteIntent)
            finish()
        }
    }
}