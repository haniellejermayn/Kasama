package com.mobicom.s18.kasama

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mobicom.s18.kasama.databinding.LayoutJoinConfirmBinding

class JoinConfirmActivity : AppCompatActivity() {
    private lateinit var viewBinding: LayoutJoinConfirmBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = LayoutJoinConfirmBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        viewBinding.backBtn.setOnClickListener {
            finish()
        }

        viewBinding.confirmBtn.setOnClickListener {
            val dashboardIntent = Intent(this, DashboardActivity::class.java)
            startActivity(dashboardIntent)
            finish()
        }
    }
}