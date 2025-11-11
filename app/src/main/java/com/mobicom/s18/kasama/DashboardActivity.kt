package com.mobicom.s18.kasama

import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.mobicom.s18.kasama.databinding.LayoutDashboardPageBinding
import com.mobicom.s18.kasama.utils.showChoreBottomSheet
import com.mobicom.s18.kasama.utils.showNoteBottomSheet
import com.mobicom.s18.kasama.viewmodels.DashboardViewModel
import kotlinx.coroutines.launch

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: LayoutDashboardPageBinding
    private lateinit var choreAdapter: ChoreAdapter
    private lateinit var noteAdapter: NoteAdapter
    private lateinit var housemateAdapter: HousemateAdapter
    private lateinit var householdAdapter: HouseholdAdapter

    private var currentTab = Tab.CHORES
    private var isSideTabOpen = false
    private lateinit var dimView: View
    private lateinit var sideTab: ConstraintLayout

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
        setupSideTab()
    }

    private fun loadUserData() {
        lifecycleScope.launch {
            val app = application as KasamaApplication
            val currentUser = app.firebaseAuth.currentUser
            if (currentUser != null) {
                currentUserId = currentUser.uid
                val userResult = app.userRepository.getUserById(currentUser.uid)
                if (userResult.isSuccess) {
                    val user = userResult.getOrNull()
                    currentHouseholdId = user?.householdId
                    currentHouseholdId?.let { householdId ->
                        viewModel.loadDashboardData(householdId)
                    }
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
                val app = application as KasamaApplication
                currentHouseholdId?.let { householdId ->
                    val householdResult = app.householdRepository.getHouseholdById(householdId)
                    if (householdResult.isSuccess) {
                        val household = householdResult.getOrNull()
                        val housemateNames = household?.memberIds?.mapNotNull { userId ->
                            app.userRepository.getUserById(userId).getOrNull()?.displayName
                        } ?: emptyList()

                        showChoreBottomSheet(
                            context = this@DashboardActivity,
                            availableHousemates = housemateNames,
                            householdId = householdId,
                            currentUserId = currentUserId ?: "",
                            chore = chore,
                            onSave = {
                                viewModel.loadDashboardData(householdId)
                            }
                        )
                    }
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
    }

    private fun setupSideTab() {
        val rootLayout = binding.dashboardPage
        sideTab = binding.sideTab

        // TODO: Double check, but right now no need for multi-household support
        // householdAdapter = HouseholdAdapter(arrayListOf())
        // binding.rvSidetabHousehold.layoutManager = LinearLayoutManager(this)
        // binding.rvSidetabHousehold.adapter = householdAdapter

        dimView = View(this).apply {
            setBackgroundColor(Color.parseColor("#80000000"))
            visibility = View.GONE
            isClickable = true
            setOnClickListener { hideSideTab() }
        }

        rootLayout.addView(dimView, ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.MATCH_PARENT
        ))
        dimView.bringToFront()

        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            private val SWIPE_THRESHOLD = 100
            private val SWIPE_VELOCITY_THRESHOLD = 100

            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (e1 == null) return false
                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y

                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0 && !isSideTabOpen) showSideTab()
                        else if (diffX < 0 && isSideTabOpen) hideSideTab()
                        return true
                    }
                }
                return false
            }
        })

        rootLayout.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }

        binding.dashboardPage.setOnClickListener {
            if (isSideTabOpen) hideSideTab()
        }
    }

    private fun showSideTab() {
        dimView.visibility = View.VISIBLE
        dimView.alpha = 0f
        dimView.animate().alpha(1f).setDuration(300).start()
        dimView.elevation = 100f

        sideTab.bringToFront()
        dimView.bringToFront()
        sideTab.visibility = View.VISIBLE
        sideTab.animate().translationX(0f).setDuration(300).start()

        isSideTabOpen = true
    }

    private fun hideSideTab() {
        dimView.animate().alpha(0f).setDuration(300).withEndAction {
            dimView.visibility = View.GONE
        }.start()

        sideTab.animate().translationX(-sideTab.width.toFloat()).setDuration(300)
            .withEndAction { sideTab.visibility = View.GONE }.start()

        isSideTabOpen = false
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
            lifecycleScope.launch {
                val app = application as KasamaApplication
                currentHouseholdId?.let { householdId ->
                    val householdResult = app.householdRepository.getHouseholdById(householdId)
                    if (householdResult.isSuccess) {
                        val household = householdResult.getOrNull()
                        val housemateNames = household?.memberIds?.mapNotNull { userId ->
                            app.userRepository.getUserById(userId).getOrNull()?.displayName
                        } ?: emptyList()

                        showChoreBottomSheet(
                            context = this@DashboardActivity,
                            availableHousemates = housemateNames,
                            householdId = householdId,
                            currentUserId = currentUserId ?: "",
                            chore = null,
                            onSave = {
                                viewModel.loadDashboardData(householdId)
                            }
                        )
                    }
                }
            }
        }

        binding.buttonViewAllChores.setOnClickListener {
            val allChoresIntent = Intent(this, ChoreActivity::class.java)
            startActivity(allChoresIntent)
        }

        binding.buttonViewAllNotes.setOnClickListener {
            val allNotesIntent = Intent(this, NoteActivity::class.java)
            startActivity(allNotesIntent)
        }

        binding.buttonNewNote.setOnClickListener {
            currentHouseholdId?.let { householdId ->
                showNoteBottomSheet(
                    context = this,
                    householdId = householdId,
                    currentUserId = currentUserId ?: "",
                    note = null,
                    onSave = {
                        viewModel.loadDashboardData(householdId)
                    }
                )
            }
        }

        binding.buttonInviteMember.setOnClickListener {
            val inviteIntent = Intent(this, InviteActivity::class.java)
            startActivity(inviteIntent)
        }

        binding.logOut.setOnClickListener {
            val app = application as KasamaApplication
            app.authRepository.logOut()
            val loginIntent = Intent(this, LoginActivity::class.java)
            loginIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(loginIntent)
            finish()
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