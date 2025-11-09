package com.mobicom.s18.kasama

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mobicom.s18.kasama.databinding.LayoutJoinPageBinding

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