/**
 *  SUBMITTED BY:
 *      CHUA, Hanielle
 *      KELSEY, Gabrielle
 *      TOLENTINO, Hephzi
 *
 *  MOBICOM S18
 */

package com.mobicom.s18.kasama

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.mobicom.s18.kasama.databinding.LayoutStarterPageBinding

class MainActivity : AppCompatActivity() {

    private lateinit var viewBinding: LayoutStarterPageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewBinding = LayoutStarterPageBinding.inflate(layoutInflater)
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