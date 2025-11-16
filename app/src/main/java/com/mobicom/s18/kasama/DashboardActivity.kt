package com.mobicom.s18.kasama

import android.animation.ObjectAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.mobicom.s18.kasama.databinding.LayoutDashboardPageBinding
import com.mobicom.s18.kasama.models.ChoreUI
import com.mobicom.s18.kasama.notifications.NotificationScheduler
import com.mobicom.s18.kasama.notifications.NotificationTester
import com.mobicom.s18.kasama.utils.PermissionHelper
import com.mobicom.s18.kasama.utils.showChoreBottomSheet
import com.mobicom.s18.kasama.utils.showNoteBottomSheet
import com.mobicom.s18.kasama.viewmodels.DashboardViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: LayoutDashboardPageBinding
    private lateinit var choreAdapter: ChoreAdapter
    private lateinit var noteAdapter: NoteAdapter
    private lateinit var housemateAdapter: HousemateAdapter

    private var currentTab = Tab.CHORES

    private val viewModel: DashboardViewModel by viewModels {
        val app = application as KasamaApplication
        DashboardViewModel.Factory(
            app.choreRepository,
            app.noteRepository,
            app.userRepository,
            app.householdRepository,
            app.database
        )
    }

    private var currentHouseholdId: String? = null
    private var currentUserId: String? = null

    enum class Tab {
        CHORES, NOTES, HOUSEMATES
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = LayoutDashboardPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerViews()
        setupTabListeners()
        setupButtonListeners()
        observeViewModel()
        loadUserData()

        showTab(Tab.CHORES, animate = false)

        // Request notification permission
        if (!PermissionHelper.checkNotificationPermission(this)) {
            PermissionHelper.requestNotificationPermission(this)
        }

        // Schedule notifications when user logs in
        NotificationScheduler.scheduleChoreReminders(this)

        handleNotificationIntent(intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PermissionHelper.NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    NotificationScheduler.scheduleChoreReminders(this)
                } else {
                    Toast.makeText(
                        this,
                        "Notification permission denied. You won't receive reminders.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.let { handleNotificationIntent(it) }
    }

    private fun handleNotificationIntent(intent: Intent) {
        val choreId = intent.getStringExtra("chore_id")
        if (choreId != null) {
            lifecycleScope.launch {
                val householdId = currentHouseholdId ?: return@launch
                val userId = currentUserId ?: return@launch

                val app = application as KasamaApplication
                val choreEntity = app.database.choreDao().getChoreByIdOnce(choreId)
                if (choreEntity != null) {
                    val assignedUser = app.userRepository.getUserById(choreEntity.assignedTo).getOrNull()
                    val chore = ChoreUI(
                        id = choreEntity.id,
                        title = choreEntity.title,
                        dueDate = SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH).format(
                            Date(choreEntity.dueDate)
                        ),
                        frequency = choreEntity.frequency ?: "Never",
                        assignedToNames = listOfNotNull(assignedUser?.displayName),
                        isCompleted = choreEntity.isCompleted
                    )

                    val household = app.householdRepository.getHouseholdById(householdId).getOrNull()
                    val housemateNames = household?.memberIds?.mapNotNull { memberUserId ->
                        app.userRepository.getUserById(memberUserId).getOrNull()?.displayName
                    } ?: emptyList()

                    showChoreBottomSheet(
                        context = this@DashboardActivity,
                        availableHousemates = housemateNames,
                        householdId = householdId,
                        currentUserId = userId,
                        chore = chore,
                        onSave = {
                            viewModel.loadDashboardData(householdId, userId)
                        }
                    )
                }
            }
        }
    }

    private fun loadUserData() {
        lifecycleScope.launch {
            val app = application as KasamaApplication
            val currentUser = app.firebaseAuth.currentUser
            if (currentUser == null) {
                return@launch
            }

            val userId = currentUser.uid
            currentUserId = userId

            val userResult = app.userRepository.getUserById(userId)
            if (userResult.isSuccess) {
                val user = userResult.getOrNull()
                val householdId = user?.householdId
                currentHouseholdId = householdId

                Log.d("Households", "User is in $householdId")

                val currHousehold = app.householdRepository.getHouseholdById(householdId.toString()).getOrNull()
                binding.textDashboardHeader.text = "${currHousehold?.name}"

                if (householdId != null) {
                    viewModel.loadDashboardData(householdId, userId)
                }
            }
        }
    }

    private fun setupRecyclerViews() {
        choreAdapter = ChoreAdapter(emptyList())
        binding.dashboardChoreRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.dashboardChoreRecyclerView.adapter = choreAdapter

        choreAdapter.setOnChoreClickListener { chore ->
            lifecycleScope.launch {
                val householdId = currentHouseholdId
                val userId = currentUserId
                if (householdId == null || userId == null) {
                    Toast.makeText(this@DashboardActivity, "User data not loaded yet", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val app = application as KasamaApplication
                val householdResult = app.householdRepository.getHouseholdById(householdId)
                if (householdResult.isSuccess) {
                    val household = householdResult.getOrNull()
                    val housemateNames = household?.memberIds?.mapNotNull { memberUserId ->
                        app.userRepository.getUserById(memberUserId).getOrNull()?.displayName
                    } ?: emptyList()

                    showChoreBottomSheet(
                        context = this@DashboardActivity,
                        availableHousemates = housemateNames,
                        householdId = householdId,
                        currentUserId = userId,
                        chore = chore,
                        onSave = {
                            viewModel.loadDashboardData(householdId, userId)
                        }
                    )
                }
            }
        }

        choreAdapter.setOnChoreCompletedListener { chore ->
            currentHouseholdId?.let { householdId ->
                viewModel.toggleChoreCompletion(chore.id, householdId)
            }
        }

        noteAdapter = NoteAdapter(mutableListOf())
        binding.dashboardNotesRecyclerView.layoutManager = GridLayoutManager(this, 3)
        binding.dashboardNotesRecyclerView.adapter = noteAdapter

        housemateAdapter = HousemateAdapter(emptyList())
        binding.dashboardHousemateRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.dashboardHousemateRecyclerView.adapter = housemateAdapter
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.chores.collect { chores ->
                choreAdapter.setChores(chores)
            }
        }

        lifecycleScope.launch {
            viewModel.notes.collect { notes ->
                noteAdapter = NoteAdapter(notes.toMutableList())
                binding.dashboardNotesRecyclerView.adapter = noteAdapter
            }
        }

        lifecycleScope.launch {
            viewModel.housemates.collect { housemates ->
                housemateAdapter = HousemateAdapter(housemates)
                binding.dashboardHousemateRecyclerView.adapter = housemateAdapter
            }
        }

        lifecycleScope.launch {
            viewModel.progressData.collect { (percentage, message, progressText) ->
                binding.circularProgress.progress = percentage
                binding.percentageDashboardProgress.text = "$percentage%"
                binding.dashboardPercentText.text = message
            }
        }

        lifecycleScope.launch {
            viewModel.mostProductiveMember.collect { (name, profileUrl) ->
                binding.productiveHousemateName.text = name

                // Load most productive member's profile picture
                if (profileUrl != null) {
                    Glide.with(this@DashboardActivity)
                        .load(profileUrl)
                        .circleCrop()
                        .placeholder(R.drawable.kasama_profile_default)
                        .error(R.drawable.kasama_profile_default)
                        .into(binding.productiveHousematePfp)
                } else {
                    binding.productiveHousematePfp.setImageResource(R.drawable.kasama_profile_default)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.recentNotesCount.collect { count ->
                binding.recentNotesCount.text = count.toString()
            }
        }
    }

    private fun setupTabListeners() {
        binding.textOptionChores.setOnClickListener {
            if (currentTab != Tab.CHORES) {
                showTab(Tab.CHORES)
            }
        }

        binding.textOptionNotes.setOnClickListener {
            if (currentTab != Tab.NOTES) {
                showTab(Tab.NOTES)
            }
        }

        binding.textOptionHousemates.setOnClickListener {
            if (currentTab != Tab.HOUSEMATES) {
                showTab(Tab.HOUSEMATES)
            }
        }
    }

    private fun setupButtonListeners() {
        binding.buttonNewChore.setOnClickListener {
            val householdId = currentHouseholdId
            val userId = currentUserId
            if (householdId == null || userId == null) {
                Toast.makeText(this, "User data not loaded yet", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val app = application as KasamaApplication
                val householdResult = app.householdRepository.getHouseholdById(householdId)
                if (householdResult.isSuccess) {
                    val household = householdResult.getOrNull()
                    val housemateNames = household?.memberIds?.mapNotNull { memberUserId ->
                        app.userRepository.getUserById(memberUserId).getOrNull()?.displayName
                    } ?: emptyList()

                    showChoreBottomSheet(
                        context = this@DashboardActivity,
                        availableHousemates = housemateNames,
                        householdId = householdId,
                        currentUserId = userId,
                        chore = null,
                        onSave = {
                            viewModel.loadDashboardData(householdId, userId)
                        }
                    )
                }
            }
        }

        binding.buttonViewAllChores.setOnClickListener {
            val allChoresIntent = Intent(this, ChoreActivity::class.java)
            allChoresIntent.putExtra("household_id", currentHouseholdId)
            allChoresIntent.putExtra("user_id", currentUserId)
            startActivity(allChoresIntent)
        }

        binding.buttonViewAllNotes.setOnClickListener {
            val allNotesIntent = Intent(this, NoteActivity::class.java)
            startActivity(allNotesIntent)
        }

        binding.buttonNewNote.setOnClickListener {
            val householdId = currentHouseholdId
            val userId = currentUserId
            if (householdId == null || userId == null) {
                Toast.makeText(this, "User data not loaded yet", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            showNoteBottomSheet(
                context = this,
                householdId = householdId,
                currentUserId = userId,
                note = null,
                onSave = {
                    viewModel.loadDashboardData(householdId, userId)
                }
            )
        }

        binding.buttonInviteMember.setOnClickListener {
            val inviteIntent = Intent(this, InviteActivity::class.java)
            startActivity(inviteIntent)
        }

        binding.logOut.setOnClickListener {
            val app = application as KasamaApplication
            app.authRepository.logOut()

            NotificationScheduler.cancelChoreReminders(this)

            val loginIntent = Intent(this, LoginActivity::class.java)
            loginIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(loginIntent)
            finish()
        }

        binding.logOut.setOnLongClickListener {
            showTestNotificationMenu()
            true
        }

        binding.buttonHome.setOnClickListener {
            val menuIntent = Intent(this, MenuActivity::class.java)
            startActivity(menuIntent)
        }

        binding.settings.setOnClickListener {
            val settingsIntent = Intent(this, SettingsActivity::class.java)
            startActivity(settingsIntent)
        }
    }

    private fun showTestNotificationMenu() {
        val items = arrayOf(
            "Test Chore Reminder",
            "Test Overdue Chore",
            "Test Household Notification",
            "Test General Notification",
            "Trigger Immediate Check",
            "View FCM Token"
        )

        android.app.AlertDialog.Builder(this)
            .setTitle("Test Notifications")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> NotificationTester.testChoreReminder(this)
                    1 -> NotificationTester.testOverdueChore(this)
                    2 -> NotificationTester.testHouseholdNotification(this)
                    3 -> NotificationTester.testGeneralNotification(this)
                    4 -> {
                        NotificationScheduler.scheduleImmediateChoreCheck(this)
                        Toast.makeText(this, "WorkManager task triggered", Toast.LENGTH_SHORT).show()
                    }
                    5 -> showFcmToken()
                }
            }
            .show()
    }

    private fun showFcmToken() {
        com.google.firebase.messaging.FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    android.app.AlertDialog.Builder(this)
                        .setTitle("FCM Token")
                        .setMessage(token)
                        .setPositiveButton("Copy") { _, _ ->
                            val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("FCM Token", token)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(this, "Token copied!", Toast.LENGTH_SHORT).show()
                        }
                        .setNegativeButton("Close", null)
                        .show()
                } else {
                    Toast.makeText(this, "Failed to get FCM token.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun showTab(tab: Tab, animate: Boolean = true) {
        currentTab = tab
        updateTabUnderlines(tab, animate)

        if (animate) {
            fadeOutCurrentContent {
                updateContentVisibility(tab)
                fadeInNewContent()
            }
        } else {
            updateContentVisibility(tab)
        }
    }

    private fun updateTabUnderlines(tab: Tab, animate: Boolean) {
        if (animate) {
            animateUnderline(binding.bottomBorderChores, tab == Tab.CHORES)
            animateUnderline(binding.bottomBorderNotes, tab == Tab.NOTES)
            animateUnderline(binding.bottomBorderHousemates, tab == Tab.HOUSEMATES)
        } else {
            binding.bottomBorderChores.visibility = if (tab == Tab.CHORES) View.VISIBLE else View.INVISIBLE
            binding.bottomBorderNotes.visibility = if (tab == Tab.NOTES) View.VISIBLE else View.INVISIBLE
            binding.bottomBorderHousemates.visibility = if (tab == Tab.HOUSEMATES) View.VISIBLE else View.INVISIBLE
        }
    }

    private fun animateUnderline(view: View, show: Boolean) {
        val alpha = if (show) 1f else 0f
        ObjectAnimator.ofFloat(view, "alpha", view.alpha, alpha).apply {
            duration = 200
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
        view.visibility = if (show) View.VISIBLE else View.INVISIBLE
    }

    private fun updateContentVisibility(tab: Tab) {
        when (tab) {
            Tab.CHORES -> {
                binding.buttonNewChore.isVisible = true
                binding.buttonViewAllChores.isVisible = true
                binding.dashboardChoreRecyclerView.isVisible = true

                binding.buttonNewNote.isVisible = false
                binding.buttonViewAllNotes.isVisible = false
                binding.dashboardNotesRecyclerView.isVisible = false
                binding.buttonInviteMember.isVisible = false
                binding.dashboardHousemateRecyclerView.isVisible = false
            }
            Tab.NOTES -> {
                binding.buttonNewNote.isVisible = true
                binding.buttonViewAllNotes.isVisible = true
                binding.dashboardNotesRecyclerView.isVisible = true

                binding.buttonNewChore.isVisible = false
                binding.buttonViewAllChores.isVisible = false
                binding.dashboardChoreRecyclerView.isVisible = false
                binding.buttonInviteMember.isVisible = false
                binding.dashboardHousemateRecyclerView.isVisible = false
            }
            Tab.HOUSEMATES -> {
                binding.buttonInviteMember.isVisible = true
                binding.dashboardHousemateRecyclerView.isVisible = true

                binding.buttonNewChore.isVisible = false
                binding.buttonViewAllChores.isVisible = false
                binding.dashboardChoreRecyclerView.isVisible = false
                binding.buttonNewNote.isVisible = false
                binding.buttonViewAllNotes.isVisible = false
                binding.dashboardNotesRecyclerView.isVisible = false
            }
        }
    }

    private fun fadeOutCurrentContent(onComplete: () -> Unit) {
        val viewsToFade = listOf(
            binding.buttonNewChore,
            binding.buttonNewNote,
            binding.buttonInviteMember,
            binding.buttonViewAllChores,
            binding.buttonViewAllNotes,
            binding.dashboardChoreRecyclerView,
            binding.dashboardNotesRecyclerView,
            binding.dashboardHousemateRecyclerView
        )

        viewsToFade.forEach { view ->
            if (view.isVisible) {
                ObjectAnimator.ofFloat(view, "alpha", 1f, 0f).apply {
                    duration = 150
                    interpolator = AccelerateDecelerateInterpolator()
                    start()
                }
            }
        }

        binding.root.postDelayed(onComplete, 150)
    }

    private fun fadeInNewContent() {
        val viewsToFade = listOf(
            binding.buttonNewChore,
            binding.buttonNewNote,
            binding.buttonInviteMember,
            binding.buttonViewAllChores,
            binding.buttonViewAllNotes,
            binding.dashboardChoreRecyclerView,
            binding.dashboardNotesRecyclerView,
            binding.dashboardHousemateRecyclerView
        )

        viewsToFade.forEach { view ->
            if (view.isVisible) {
                view.alpha = 0f
                ObjectAnimator.ofFloat(view, "alpha", 0f, 1f).apply {
                    duration = 150
                    interpolator = AccelerateDecelerateInterpolator()
                    start()
                }
            }
        }
    }
}