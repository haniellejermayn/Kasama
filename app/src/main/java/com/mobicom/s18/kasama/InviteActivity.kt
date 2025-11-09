package com.mobicom.s18.kasama

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mobicom.s18.kasama.databinding.LayoutInvitePageBinding

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