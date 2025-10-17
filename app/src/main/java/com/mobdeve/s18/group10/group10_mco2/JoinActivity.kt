package com.mobdeve.s18.group10.group10_mco2

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mobdeve.s18.group10.group10_mco2.databinding.LayoutJoinPageBinding

class JoinActivity : AppCompatActivity(){
    private lateinit var viewBinding: LayoutJoinPageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = LayoutJoinPageBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        viewBinding.cancelBtn.setOnClickListener {
            finish()
        }

        viewBinding.joinBtn.setOnClickListener {
            val joinConfirmIntent = Intent(this, JoinConfirmActivity::class.java)
            startActivity(joinConfirmIntent)
        }
    }
}