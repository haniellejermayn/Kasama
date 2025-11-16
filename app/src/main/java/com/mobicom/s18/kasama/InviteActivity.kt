package com.mobicom.s18.kasama

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.mobicom.s18.kasama.databinding.LayoutInvitePageBinding
import kotlinx.coroutines.launch
import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix

class InviteActivity : AppCompatActivity() {
    private lateinit var viewBinding: LayoutInvitePageBinding
    private lateinit var householdName: String
    private lateinit var inviteCode: String
    private lateinit var householdId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = LayoutInvitePageBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        // Check if household data was passed via intent (from CreateHouseholdActivity)
        if (intent.hasExtra("HOUSEHOLD_ID")) {
            householdName = intent.getStringExtra("HOUSEHOLD_NAME") ?: "Household"
            inviteCode = intent.getStringExtra("INVITE_CODE") ?: ""
            householdId = intent.getStringExtra("HOUSEHOLD_ID") ?: ""

            setupUI()
        } else {
            // Load from current user's household (from DashboardActivity)
            loadCurrentHouseholdData()
        }

        setupListeners()
    }

    private fun loadCurrentHouseholdData() {
        lifecycleScope.launch {
            val app = application as KasamaApplication
            val currentUser = app.firebaseAuth.currentUser

            if (currentUser == null) {
                Toast.makeText(this@InviteActivity, "User not logged in", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }

            // Get user's household ID
            val userResult = app.userRepository.getUserById(currentUser.uid)
            if (userResult.isFailure || userResult.getOrNull()?.householdId == null) {
                Toast.makeText(
                    this@InviteActivity,
                    "No household found. Please create one first.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
                return@launch
            }

            val user = userResult.getOrNull()!!
            householdId = user.householdId!!

            // Get household details
            val householdResult = app.householdRepository.getHouseholdById(householdId)
            if (householdResult.isFailure) {
                Toast.makeText(
                    this@InviteActivity,
                    "Failed to load household data",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
                return@launch
            }

            val household = householdResult.getOrNull()!!
            householdName = household.name
            inviteCode = household.inviteCode

            setupUI()
        }
    }

    private fun setupUI() {
        viewBinding.headerTv.text = "Welcome to $householdName!"
        viewBinding.codeTv.text = inviteCode
        viewBinding.linkTv.text = "kasama.app/join/$inviteCode"

        val qrBitmap = generateQRCode(inviteCode)
        viewBinding.qrImg.setImageBitmap(qrBitmap)
    }

    private fun setupListeners() {
        viewBinding.goHomeBtn.setOnClickListener {
            val dashboardIntent = Intent(this, DashboardActivity::class.java)
            dashboardIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(dashboardIntent)
            finish()
        }

        viewBinding.codeTv.setOnClickListener {
            if (::inviteCode.isInitialized) {
                copyToClipboard(inviteCode, "Invite code copied!")
            }
        }

        viewBinding.linkTv.setOnClickListener {
            if (::inviteCode.isInitialized) {
                copyToClipboard("kasama.app/join/$inviteCode", "Invite link copied!")
            }
        }

        viewBinding.shareTv.setOnClickListener {
            if (::householdName.isInitialized && ::inviteCode.isInitialized) {
                shareHousehold()
            } else {
                Toast.makeText(this, "Household data not loaded yet", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun copyToClipboard(text: String, message: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Kasama Invite", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun shareHousehold() {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Join my household on Kasama!")
            putExtra(
                Intent.EXTRA_TEXT,
                "Join '$householdName' on Kasama!\n\n" +
                        "Invite Code: $inviteCode\n" +
                        "Link: kasama.app/join/$inviteCode"
            )
        }
        startActivity(Intent.createChooser(shareIntent, "Share household invite"))
    }

    fun generateQRCode(text: String, size: Int = 512): Bitmap {
        val bitMatrix: BitMatrix = MultiFormatWriter().encode(
            text,
            BarcodeFormat.QR_CODE,
            size,
            size
        )

        val width = bitMatrix.width
        val height = bitMatrix.height
        var bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

        for (x in 0 until width) {
            for (y in 0 until height) {
                bmp.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }

        return bmp
    }
}