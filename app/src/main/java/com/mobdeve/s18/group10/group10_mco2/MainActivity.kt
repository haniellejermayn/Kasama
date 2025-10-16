package com.mobdeve.s18.group10.group10_mco2

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import com.mobdeve.s18.group10.group10_mco2.databinding.LayoutStarterPageBinding

class MainActivity : AppCompatActivity() {

    private lateinit var viewBinding: ViewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val viewBinding : LayoutStarterPageBinding = LayoutStarterPageBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        viewBinding.buttonSignUp.setOnClickListener {
            val signupIntent = Intent(this, SignupActivity::class.java)
            startActivity(signupIntent)
        }
        viewBinding.buttonLogIn.setOnClickListener {
            val loginIntent = Intent(this, LoginActivity::class.java)
            startActivity(loginIntent)
        }
    }
}