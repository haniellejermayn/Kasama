package com.mobdeve.s18.group10.group10_mco2

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mobdeve.s18.group10.group10_mco2.databinding.LayoutProfileSetupBinding
import java.util.Calendar

class ProfileSetupActivity : AppCompatActivity() {
    private lateinit var viewBinding: LayoutProfileSetupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = LayoutProfileSetupBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        viewBinding.buttonConfirm.setOnClickListener {
            val householdSetupIntent = Intent(this, HouseholdSetupActivity::class.java)
            startActivity(householdSetupIntent)
            finish()
        }

        viewBinding.imagebtnAddImage.setOnClickListener {
            // TODO: file upload feature
            Toast.makeText(this, "[TEMPORARY] Must be able to file upload...", Toast.LENGTH_SHORT).show()
        }

        viewBinding.edittextBirthdate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePicker = DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->

                    val formattedDate = "${selectedMonth + 1}/$selectedDay/$selectedYear"
                    viewBinding.edittextBirthdate.setText(formattedDate)
                },
                year,
                month,
                day
            )

            datePicker.datePicker.maxDate = System.currentTimeMillis()
            datePicker.show()
        }
    }
}