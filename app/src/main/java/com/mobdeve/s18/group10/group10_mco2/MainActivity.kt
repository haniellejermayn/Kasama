package com.mobdeve.s18.group10.group10_mco2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.mobdeve.s18.group10.group10_mco2.databinding.LayoutMainBinding
import com.mobdeve.s18.group10.group10_mco2.ui.theme.Group10MCO2Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val viewBinding : LayoutMainBinding = LayoutMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
    }
}