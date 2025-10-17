package com.mobdeve.s18.group10.group10_mco2

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mobdeve.s18.group10.group10_mco2.databinding.LayoutInvitePageBinding

class InviteActivity : AppCompatActivity(){
    private lateinit var viewBinding: LayoutInvitePageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = LayoutInvitePageBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        viewBinding.goHomeBtn.setOnClickListener {
            val dashboardIntent = Intent(this, DashboardActivity::class.java)
            startActivity(dashboardIntent)
            finish()
        }
    }
}