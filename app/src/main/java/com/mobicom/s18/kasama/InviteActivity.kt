package com.mobicom.s18.kasama

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mobicom.s18.kasama.databinding.LayoutInvitePageBinding

class InviteActivity : AppCompatActivity() {
    private lateinit var viewBinding: LayoutInvitePageBinding
    private lateinit var householdName: String
    private lateinit var inviteCode: String
    private lateinit var householdId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = LayoutInvitePageBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        householdName = intent.getStringExtra("HOUSEHOLD_NAME") ?: "Household"
        inviteCode = intent.getStringExtra("INVITE_CODE") ?: ""
        householdId = intent.getStringExtra("HOUSEHOLD_ID") ?: ""

        viewBinding.headerTv.text = "Welcome to $householdName!"
        viewBinding.codeTv.text = inviteCode
        viewBinding.linkTv.text = "kasama.app/join/$inviteCode"

        // TODO: Generate QR code

        viewBinding.goHomeBtn.setOnClickListener {
            val dashboardIntent = Intent(this, DashboardActivity::class.java)
            startActivity(dashboardIntent)
            finish()
        }

        viewBinding.codeTv.setOnClickListener {
            copyToClipboard(inviteCode, "Invite code copied!")
        }

        viewBinding.linkTv.setOnClickListener {
            copyToClipboard("kasama.app/join/$inviteCode", "Invite link copied!")
        }

        viewBinding.shareTv.setOnClickListener {
            shareHousehold()
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
}