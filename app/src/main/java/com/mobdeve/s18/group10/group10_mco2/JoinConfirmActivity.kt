package com.mobdeve.s18.group10.group10_mco2

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mobdeve.s18.group10.group10_mco2.databinding.LayoutJoinConfirmBinding

class JoinConfirmActivity : AppCompatActivity() {
    private lateinit var viewBinding: LayoutJoinConfirmBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = LayoutJoinConfirmBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        viewBinding.backBtn.setOnClickListener {
            finish()
        }
    }
}